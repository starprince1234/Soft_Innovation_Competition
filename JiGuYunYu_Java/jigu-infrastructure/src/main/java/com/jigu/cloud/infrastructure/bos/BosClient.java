package com.jigu.cloud.infrastructure.bos;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.OSSObject;
import com.baidubce.auth.DefaultBceCredentials;
import com.baidubce.services.bos.BosClientConfiguration;
import com.baidubce.services.bos.model.ObjectMetadata;
import com.jigu.cloud.common.enums.ErrorCode;
import com.jigu.cloud.common.exception.BizException;
import com.jigu.cloud.infrastructure.config.BosProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Base64;
import java.util.UUID;
import java.net.UnknownHostException;

/**
 * BOS（百度对象存储）封装客户端。
 * <p>
 * 统一处理上传/下载/生成 URL。
 * <ul>
 *   <li>BOS 已配置 → 使用 bce-java-sdk 真实调用</li>
 *   <li>BOS 未配置 → 降级为 mock URL（本地开发友好）</li>
 * </ul>
 */
@Component
public class BosClient {

    private static final Logger log = LoggerFactory.getLogger(BosClient.class);
    private static final long ALIYUN_HEALTH_RETRY_INTERVAL_MS = 300_000L;

    private final BosProperties bosProperties;

    /** bce-java-sdk 的 BOS 客户端实例（SDK 类名也叫 BosClient，使用全限定名避免冲突） */
    private com.baidubce.services.bos.BosClient bceBosClient;
    private OSS aliyunClient;
    private String normalizedEndpoint;
    private String aliyunSdkEndpoint;
    private boolean aliyunMode;
    private volatile long lastAliyunHealthCheckAt;
    private volatile Boolean lastAliyunHealthStatus;
    private volatile boolean aliyunHealthAclDenied;

    public BosClient(BosProperties bosProperties) {
        this.bosProperties = bosProperties;
    }

    @PostConstruct
    public void init() {
        normalizedEndpoint = normalizeEndpoint(bosProperties.getEndpoint());
        aliyunMode = isAliyunEndpoint(normalizedEndpoint);
        aliyunSdkEndpoint = normalizedEndpoint;
        if (aliyunMode && normalizedEndpoint.contains("-internal.")) {
            aliyunSdkEndpoint = normalizedEndpoint.replace("-internal.", ".");
        }

        if (bosProperties.isConfigured()) {
            try {
                if (aliyunMode) {
                    this.aliyunClient = new OSSClientBuilder().build(
                            aliyunSdkEndpoint,
                            bosProperties.getAccessKey(),
                            bosProperties.getSecretKey()
                    );
                    this.bceBosClient = null;
                    log.info("Aliyun OSS client initialized: endpoint={}, bucket={}",
                            normalizedEndpoint, bosProperties.getBucket());
                } else {
                    BosClientConfiguration config = new BosClientConfiguration();
                    config.setCredentials(new DefaultBceCredentials(
                            bosProperties.getAccessKey(),
                            bosProperties.getSecretKey()
                    ));
                    config.setEndpoint(normalizedEndpoint);
                    this.bceBosClient = new com.baidubce.services.bos.BosClient(config);
                    this.aliyunClient = null;
                    log.info("BOS client initialized: endpoint={}, bucket={}",
                            normalizedEndpoint, bosProperties.getBucket());
                }
            } catch (Exception e) {
                log.error("BOS client initialization failed: {}", e.getMessage(), e);
                this.bceBosClient = null;
                this.aliyunClient = null;
            }
        } else {
            log.warn("BOS not configured, using mock mode");
        }
    }

    /**
     * 上传 Base64 编码的文件到 BOS。
     *
     * @param base64Data Base64 编码数据（不含前缀如 data:image/png;base64,）
     * @param directory  存储目录（如 detect/, screenshot/）
     * @param extension  文件扩展名（如 .png, .jpg）
     * @return 文件访问 URL
     */
    public String uploadBase64(String base64Data, String directory, String extension) {
        if (base64Data == null || base64Data.isBlank()) {
            throw new BizException(ErrorCode.BAD_REQUEST, "上传数据不能为空");
        }

        // 去除可能的 data URI 前缀
        String pureBase64 = base64Data;
        if (pureBase64.contains(",")) {
            pureBase64 = pureBase64.substring(pureBase64.indexOf(",") + 1);
        }

        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(pureBase64);
        } catch (IllegalArgumentException e) {
            throw new BizException(ErrorCode.BAD_REQUEST, "无效的 Base64 编码");
        }

        // 大小校验
        if (bytes.length > bosProperties.getMaxFileSize()) {
            throw new BizException(ErrorCode.DETECT_IMAGE_TOO_LARGE,
                    "文件大小超出限制: " + bytes.length + " > " + bosProperties.getMaxFileSize());
        }

        return uploadBytes(bytes, directory, extension);
    }

    /**
     * 上传字节数据到 BOS。
     *
     * @return 文件访问 URL
     */
    public String uploadBytes(byte[] data, String directory, String extension) {
        String normalizedDirectory = directory == null ? "" : directory.trim();
        if (!normalizedDirectory.isEmpty() && !normalizedDirectory.endsWith("/")) {
            normalizedDirectory = normalizedDirectory + "/";
        }
        String objectKey = normalizedDirectory + UUID.randomUUID() + extension;

        if (!bosProperties.isConfigured()) {
            // BOS 未配置时使用本地模拟 URL
            String mockUrl = "https://bos-mock.jigu.cloud/" + bosProperties.getBucket() + "/" + objectKey;
            log.warn("BOS not configured, using mock URL: {}", mockUrl);
            return mockUrl;
        }

        if (aliyunMode) {
            return uploadAliyunObject(objectKey, data, extension);
        }

        try {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(data.length);
            metadata.setContentType(guessContentType(extension));

            InputStream inputStream = new ByteArrayInputStream(data);
            bceBosClient.putObject(bosProperties.getBucket(), objectKey, inputStream, metadata);

            String url = bosProperties.getEndpoint() + "/" + bosProperties.getBucket() + "/" + objectKey;
            log.info("File uploaded to BOS: key={}, size={}bytes, url={}", objectKey, data.length, url);
            return url;
        } catch (Exception e) {
            log.error("BOS upload failed: key={}, error={}", objectKey, e.getMessage(), e);
            throw new BizException(ErrorCode.BOS_UPLOAD_FAILED, "文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 从 BOS 下载文件字节数据。
     *
     * @param objectKey 对象 key（如 detect/xxx.jpg）
     * @return 文件字节内容，BOS 未配置或对象不存在时返回空数组
     */
    public byte[] downloadBytes(String objectKey) {
        if (!bosProperties.isConfigured()) {
            log.debug("BOS not configured, returning empty bytes for key={}", objectKey);
            return new byte[0];
        }

        if (aliyunMode) {
            return downloadAliyunObject(objectKey);
        }

        try {
            var bosObject = bceBosClient.getObject(bosProperties.getBucket(), objectKey);
            try (InputStream is = bosObject.getObjectContent()) {
                return is.readAllBytes();
            }
        } catch (Exception e) {
            log.warn("BOS download failed: key={}, error={}", objectKey, e.getMessage());
            return new byte[0];
        }
    }

    /**
     * 健康检查。
     */
    public boolean isHealthy() {
        if (!bosProperties.isConfigured()) {
            return false;
        }

        if (aliyunMode) {
            return isAliyunHealthy();
        }

        if (bceBosClient == null) {
            return false;
        }

        try {
            bceBosClient.doesBucketExist(bosProperties.getBucket());
            return true;
        } catch (Exception e) {
            log.warn("BOS health check failed: {}", e.getMessage());
            return false;
        }
    }

    // ---- 内部方法 ----

    private static String guessContentType(String extension) {
        return switch (extension.toLowerCase()) {
            case ".png" -> "image/png";
            case ".jpg", ".jpeg" -> "image/jpeg";
            case ".webp" -> "image/webp";
            case ".gif" -> "image/gif";
            case ".txt" -> "text/plain; charset=UTF-8";
            case ".json" -> "application/json; charset=UTF-8";
            default -> "application/octet-stream";
        };
    }

    private String uploadAliyunObject(String objectKey, byte[] data, String extension) {
        if (aliyunClient == null) {
            throw new BizException(ErrorCode.BOS_UPLOAD_FAILED, "Aliyun OSS client not initialized");
        }

        try {
            com.aliyun.oss.model.ObjectMetadata metadata = new com.aliyun.oss.model.ObjectMetadata();
            metadata.setContentLength(data.length);
            metadata.setContentType(guessContentType(extension));

            InputStream inputStream = new ByteArrayInputStream(data);
            aliyunClient.putObject(bosProperties.getBucket(), objectKey, inputStream, metadata);

            String url = publicAliyunEndpoint() + "/" + objectKey;
            log.info("File uploaded to Aliyun OSS: key={}, size={}bytes, url={}", objectKey, data.length, url);
            return url;
        } catch (Exception e) {
            if (hasUnknownHost(e) && rebuildAliyunClientWithPublicEndpoint()) {
                try {
                    com.aliyun.oss.model.ObjectMetadata metadata = new com.aliyun.oss.model.ObjectMetadata();
                    metadata.setContentLength(data.length);
                    metadata.setContentType(guessContentType(extension));
                    InputStream retryInputStream = new ByteArrayInputStream(data);
                    aliyunClient.putObject(bosProperties.getBucket(), objectKey, retryInputStream, metadata);

                    String url = publicAliyunEndpoint() + "/" + objectKey;
                    log.info("Aliyun OSS upload recovered after endpoint fallback: key={}, endpoint={}", objectKey, aliyunSdkEndpoint);
                    return url;
                } catch (Exception retryEx) {
                    throw new BizException(ErrorCode.BOS_UPLOAD_FAILED, "文件上传失败: " + retryEx.getMessage());
                }
            }
            throw new BizException(ErrorCode.BOS_UPLOAD_FAILED, "文件上传失败: " + e.getMessage());
        }
    }

    private byte[] downloadAliyunObject(String objectKey) {
        if (aliyunClient == null) {
            return new byte[0];
        }

        try {
            OSSObject object = aliyunClient.getObject(bosProperties.getBucket(), objectKey);
            try (InputStream is = object.getObjectContent()) {
                return is.readAllBytes();
            }
        } catch (Exception e) {
            if (hasUnknownHost(e) && rebuildAliyunClientWithPublicEndpoint()) {
                try {
                    OSSObject retryObject = aliyunClient.getObject(bosProperties.getBucket(), objectKey);
                    try (InputStream is = retryObject.getObjectContent()) {
                        return is.readAllBytes();
                    }
                } catch (Exception retryEx) {
                    log.warn("Aliyun OSS download retry failed: key={}, endpoint={}, error={}", objectKey, aliyunSdkEndpoint, retryEx.getMessage());
                    return new byte[0];
                }
            }
            log.warn("Aliyun OSS download failed: key={}, error={}", objectKey, e.getMessage());
        }
        return new byte[0];
    }

    private boolean isAliyunHealthy() {
        if (aliyunClient == null) {
            return false;
        }

        // Avoid ACL-based bucket probe by default because some sub-accounts
        // are allowed object read/write but denied GetBucketAcl on health checks.
        if (!bosProperties.isHealthCheckBucketExists()) {
            return true;
        }

        long now = System.currentTimeMillis();
        if (aliyunHealthAclDenied
                && lastAliyunHealthStatus != null
                && (now - lastAliyunHealthCheckAt) < ALIYUN_HEALTH_RETRY_INTERVAL_MS) {
            return lastAliyunHealthStatus;
        }

        try {
            boolean healthy = aliyunClient.doesBucketExist(bosProperties.getBucket());
            lastAliyunHealthStatus = healthy;
            lastAliyunHealthCheckAt = now;
            aliyunHealthAclDenied = false;
            return healthy;
        } catch (OSSException e) {
            if ("AccessDenied".equalsIgnoreCase(e.getErrorCode())) {
                if (!aliyunHealthAclDenied) {
                    log.warn("Aliyun OSS health check lacks bucket ACL permission, degrade to client-initialized check. bucket={}",
                            bosProperties.getBucket());
                }
                // Some sub-accounts can read/write objects but are denied bucket ACL checks.
                aliyunHealthAclDenied = true;
                lastAliyunHealthStatus = true;
                lastAliyunHealthCheckAt = now;
                return true;
            }
            log.warn("Aliyun OSS health check failed: endpoint={}, code={}, error={}", aliyunSdkEndpoint, e.getErrorCode(), e.getMessage());
            aliyunHealthAclDenied = false;
            lastAliyunHealthStatus = false;
            lastAliyunHealthCheckAt = now;
            return false;
        } catch (Exception e) {
            log.warn("Aliyun OSS health check failed: endpoint={}, error={}", aliyunSdkEndpoint, e.getMessage());
            String publicEndpoint = publicAliyunEndpointWithoutBucket();
            if (publicEndpoint.equals(aliyunSdkEndpoint)) {
                aliyunHealthAclDenied = false;
                lastAliyunHealthStatus = false;
                lastAliyunHealthCheckAt = now;
                return false;
            }
            try {
                OSS publicClient = new OSSClientBuilder().build(
                        publicEndpoint,
                        bosProperties.getAccessKey(),
                        bosProperties.getSecretKey()
                );
                try {
                    boolean healthy = publicClient.doesBucketExist(bosProperties.getBucket());
                    lastAliyunHealthStatus = healthy;
                    lastAliyunHealthCheckAt = now;
                    aliyunHealthAclDenied = false;
                    return healthy;
                } finally {
                    publicClient.shutdown();
                }
            } catch (Exception ex) {
                log.warn("Aliyun OSS health fallback failed: endpoint={}, error={}", publicEndpoint, ex.getMessage());
                aliyunHealthAclDenied = false;
                lastAliyunHealthStatus = false;
                lastAliyunHealthCheckAt = now;
                return false;
            }
        }
    }

    private boolean rebuildAliyunClientWithPublicEndpoint() {
        if (!aliyunMode) {
            return false;
        }
        String publicEndpoint = publicAliyunEndpointWithoutBucket();
        if (publicEndpoint == null || publicEndpoint.isBlank()) {
            return false;
        }
        try {
            if (aliyunClient != null) {
                aliyunClient.shutdown();
            }
            aliyunSdkEndpoint = publicEndpoint;
            aliyunClient = new OSSClientBuilder().build(
                    aliyunSdkEndpoint,
                    bosProperties.getAccessKey(),
                    bosProperties.getSecretKey()
            );
            aliyunHealthAclDenied = false;
            lastAliyunHealthStatus = null;
            lastAliyunHealthCheckAt = 0L;
            log.warn("Aliyun OSS client rebuilt with public endpoint fallback: {}", aliyunSdkEndpoint);
            return true;
        } catch (Exception ex) {
            log.warn("Aliyun OSS client rebuild failed: endpoint={}, error={}", publicEndpoint, ex.getMessage());
            return false;
        }
    }

    private static boolean hasUnknownHost(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof UnknownHostException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private static String normalizeEndpoint(String endpoint) {
        if (endpoint == null || endpoint.isBlank()) {
            return "";
        }
        String normalized = endpoint.trim();
        if (!normalized.startsWith("http://") && !normalized.startsWith("https://")) {
            normalized = "https://" + normalized;
        }
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private static boolean isAliyunEndpoint(String endpoint) {
        return endpoint != null && endpoint.contains("aliyuncs.com");
    }

    private String publicAliyunEndpointWithoutBucket() {
        return normalizedEndpoint.replace("-internal.", ".");
    }

    private String publicAliyunEndpoint() {
        String endpoint = publicAliyunEndpointWithoutBucket();
        String withoutScheme = endpoint
                .replaceFirst("^https?://", "")
                .replaceAll("/$", "");
        return "https://" + bosProperties.getBucket() + "." + withoutScheme;
    }
}
