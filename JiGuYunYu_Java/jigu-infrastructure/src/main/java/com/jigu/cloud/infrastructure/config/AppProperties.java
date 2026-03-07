package com.jigu.cloud.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 全局应用配置属性。
 */
@Component
@ConfigurationProperties(prefix = "jigu.app")
public class AppProperties {

    /** 允许的图片格式 */
    private String[] allowedImageTypes = {"image/jpeg", "image/png", "image/webp"};

    /** 截图最大大小（字节），默认 2MB */
    private long maxScreenshotSize = 2 * 1024 * 1024;

    /** 检测图片最大大小（字节），默认 10MB */
    private long maxDetectImageSize = 10 * 1024 * 1024;

    public String[] getAllowedImageTypes() { return allowedImageTypes; }
    public void setAllowedImageTypes(String[] allowedImageTypes) { this.allowedImageTypes = allowedImageTypes; }

    public long getMaxScreenshotSize() { return maxScreenshotSize; }
    public void setMaxScreenshotSize(long maxScreenshotSize) { this.maxScreenshotSize = maxScreenshotSize; }

    public long getMaxDetectImageSize() { return maxDetectImageSize; }
    public void setMaxDetectImageSize(long maxDetectImageSize) { this.maxDetectImageSize = maxDetectImageSize; }
}
