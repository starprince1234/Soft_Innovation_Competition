package com.jigu.cloud.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Redis 配置属性（Spring Data Redis 自动装配，额外自定义配置放此处）。
 */
@Component
@ConfigurationProperties(prefix = "jigu.redis")
public class RedisProperties {

    /** JWT 黑名单 key 前缀 */
    private String jwtBlacklistPrefix = "jwt_blacklist:";

    public String getJwtBlacklistPrefix() { return jwtBlacklistPrefix; }
    public void setJwtBlacklistPrefix(String jwtBlacklistPrefix) { this.jwtBlacklistPrefix = jwtBlacklistPrefix; }
}
