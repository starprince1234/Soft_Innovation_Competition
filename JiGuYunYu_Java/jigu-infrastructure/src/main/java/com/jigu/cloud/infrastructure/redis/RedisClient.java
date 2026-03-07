package com.jigu.cloud.infrastructure.redis;

import com.jigu.cloud.infrastructure.config.RedisProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Redis 操作封装。
 * <p>
 * 核心能力：JWT 黑名单、通用缓存。
 * Redis 故障时 JWT 黑名单不降级（安全第一），其他缓存可降级。
 */
@Component
public class RedisClient {

    private static final Logger log = LoggerFactory.getLogger(RedisClient.class);

    private final StringRedisTemplate redisTemplate;
    private final RedisProperties redisProperties;

    public RedisClient(StringRedisTemplate redisTemplate, RedisProperties redisProperties) {
        this.redisTemplate = redisTemplate;
        this.redisProperties = redisProperties;
    }

    // -------- JWT 黑名单 --------

    /**
     * 将 Token 哈希加入黑名单。
     *
     * @param tokenHash SHA-256 哈希
     * @param ttlSeconds 剩余有效期（秒）
     */
    public void addToBlacklist(String tokenHash, long ttlSeconds) {
        String key = redisProperties.getJwtBlacklistPrefix() + tokenHash;
        redisTemplate.opsForValue().set(key, "1", ttlSeconds, TimeUnit.SECONDS);
        log.debug("JWT token added to blacklist, ttl={}s", ttlSeconds);
    }

    /**
     * 检查 Token 是否在黑名单中。
     *
     * @return true = 已被拉黑（已登出）
     */
    public boolean isBlacklisted(String tokenHash) {
        String key = redisProperties.getJwtBlacklistPrefix() + tokenHash;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    // -------- 通用操作 --------

    public void set(String key, String value, long ttlSeconds) {
        redisTemplate.opsForValue().set(key, value, ttlSeconds, TimeUnit.SECONDS);
    }

    public String get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    public boolean exists(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    public void delete(String key) {
        redisTemplate.delete(key);
    }

    // -------- API 限流 --------

    /**
     * 基于滑动窗口的 API 限流。
     * <p>
     * Redis key 格式：{@code api_rate_limit:{userId}:{apiPath}}
     * 使用 INCR + EXPIRE 实现固定窗口计数器。
     *
     * @param userId   用户 ID
     * @param apiPath  API 路径（如 /api/v1/dialog）
     * @param maxRequests 窗口内最大请求数
     * @param windowSeconds 窗口时间（秒）
     * @return true = 已超限（应拒绝请求）
     */
    public boolean isRateLimited(Long userId, String apiPath, int maxRequests, int windowSeconds) {
        String key = "api_rate_limit:" + userId + ":" + apiPath;
        try {
            Long count = redisTemplate.opsForValue().increment(key);
            if (count != null && count == 1) {
                // 首次请求，设置过期时间
                redisTemplate.expire(key, windowSeconds, TimeUnit.SECONDS);
            }
            boolean limited = count != null && count > maxRequests;
            if (limited) {
                log.warn("Rate limit exceeded: userId={}, path={}, count={}/{}", userId, apiPath, count, maxRequests);
            }
            return limited;
        } catch (Exception e) {
            // Redis 故障时不限流（降级策略，避免拒绝正常请求）
            log.warn("Rate limit check failed (degraded to pass): {}", e.getMessage());
            return false;
        }
    }

    /**
     * 健康检查。
     */
    public boolean isHealthy() {
        try {
            String result = redisTemplate.getConnectionFactory()
                    .getConnection().ping();
            return "PONG".equalsIgnoreCase(result);
        } catch (Exception e) {
            log.warn("Redis health check failed: {}", e.getMessage());
            return false;
        }
    }
}
