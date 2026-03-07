package com.jigu.cloud.infrastructure.bos;

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

    private final BosProperties bosProperties;

    /** bce-java-sdk 的 BOS 客户端实例（SDK 类名也叫 BosClient，使用全限定名避免冲突） */
    private com.baidubce.services.bos.BosClient bceBosClient;

    public BosClient(BosProperties bosProperties) {
        this.bosProperties = bosProperties;
    }

    @PostConstruct
    public void init() {
        if (bosProperties.isConfigured()) {
            try {
                BosClientConfiguration config = new BosClientConfiguration();
                config.setCredentials(new DefaultBceCredentials(
                        bosProperties.getAccessKey(),
                        bosProperties.getSecretKey()
                ));
                config.setEndpoint(bosProperties.getEndpoint());
                this.bceBosClient = new com.baidubce.services.bos.BosClient(config);
                log.info("BOS client initialized: endpoint={}, bucket={}",
                        bosProperties.getEndpoint(), bosProperties.getBucket());
            } catch (Exception e) {
                log.error("BOS client initialization failed: {}", e.getMessage(), e);
                this.bceBosClient = null;
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
        String objectKey = directory + UUID.randomUUID() + extension;

        if (bceBosClient == null) {
            // BOS 未配置时使用本地模拟 URL
            String mockUrl = "https://bos-mock.jigu.cloud/" + bosProperties.getBucket() + "/" + objectKey;
            log.warn("BOS not configured, using mock URL: {}", mockUrl);
            return mockUrl;
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
        if (bceBosClient == null) {
            log.debug("BOS not configured, returning empty bytes for key={}", objectKey);
            return new byte[0];
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
            default -> "application/octet-stream";
        };
    }
}
