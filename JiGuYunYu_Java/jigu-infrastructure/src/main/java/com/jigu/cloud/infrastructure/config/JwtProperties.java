package com.jigu.cloud.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT 配置属性（对应 application.yml 中 jigu.jwt.*）。
 * <p>
 * secret 只能从环境变量注入，禁止硬编码。
 */
@Component
@ConfigurationProperties(prefix = "jigu.jwt")
public class JwtProperties {

    /** JWT 签名密钥（Base64 编码或长随机串） */
    private String secret = "change_me";

    /** Token 有效期（秒） */
    private long expiresSeconds = 3600;

    /** 签发者 */
    private String issuer = "jigu-cloud";

    public String getSecret() { return secret; }
    public void setSecret(String secret) { this.secret = secret; }

    public long getExpiresSeconds() { return expiresSeconds; }
    public void setExpiresSeconds(long expiresSeconds) { this.expiresSeconds = expiresSeconds; }

    public String getIssuer() { return issuer; }
    public void setIssuer(String issuer) { this.issuer = issuer; }
}
