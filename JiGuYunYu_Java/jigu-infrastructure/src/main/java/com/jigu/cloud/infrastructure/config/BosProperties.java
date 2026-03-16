package com.jigu.cloud.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * BOS 对象存储配置属性（对应 application.yml 中 jigu.bos.*）。
 * <p>
 * 密钥必须来自环境变量/密钥管理，不写死在 yml。
 */
@Component
@ConfigurationProperties(prefix = "jigu.bos")
public class BosProperties {

    private String endpoint = "";
    private String accessKey = "";
    private String secretKey = "";
    private String bucket = "";

    /** 上传文件最大大小（字节），默认 5MB */
    private long maxFileSize = 5 * 1024 * 1024;

    /**
     * 是否在健康检查中执行 bucket exists 探测。
     * 对阿里云子账号场景，doesBucketExist 可能触发 GetBucketAcl 并被拒绝，建议默认关闭。
     */
    private boolean healthCheckBucketExists = false;

    public String getEndpoint() { return endpoint; }
    public void setEndpoint(String endpoint) { this.endpoint = endpoint; }

    public String getAccessKey() { return accessKey; }
    public void setAccessKey(String accessKey) { this.accessKey = accessKey; }

    public String getSecretKey() { return secretKey; }
    public void setSecretKey(String secretKey) { this.secretKey = secretKey; }

    public String getBucket() { return bucket; }
    public void setBucket(String bucket) { this.bucket = bucket; }

    public long getMaxFileSize() { return maxFileSize; }
    public void setMaxFileSize(long maxFileSize) { this.maxFileSize = maxFileSize; }

    public boolean isHealthCheckBucketExists() { return healthCheckBucketExists; }
    public void setHealthCheckBucketExists(boolean healthCheckBucketExists) { this.healthCheckBucketExists = healthCheckBucketExists; }

    /** 判断 BOS 是否已配置 */
    public boolean isConfigured() {
        return endpoint != null && !endpoint.isBlank()
                && accessKey != null && !accessKey.isBlank()
                && secretKey != null && !secretKey.isBlank()
                && bucket != null && !bucket.isBlank();
    }
}
