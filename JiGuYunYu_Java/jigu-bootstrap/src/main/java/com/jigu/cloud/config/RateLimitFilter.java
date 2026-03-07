package com.jigu.cloud.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jigu.cloud.common.response.ApiResponse;
import com.jigu.cloud.infrastructure.redis.RedisClient;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * API 限流过滤器。
 * <p>
 * 基于 Redis 固定窗口计数器，key 格式：{@code api_rate_limit:{userId}:{apiPath}}。
 * <p>
 * 仅对已认证用户（userId 已通过 JWT 过滤器写入 request attribute）的业务接口生效。
 * 公开接口、内部接口、认证接口不受限流。
 * <p>
 * 默认策略：60 秒内最多 60 次请求（可通过配置调整）。
 */
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    /** 窗口内最大请求数 */
    private static final int MAX_REQUESTS = 60;
    /** 窗口时间（秒） */
    private static final int WINDOW_SECONDS = 60;

    private final RedisClient redisClient;
    private final ObjectMapper objectMapper;

    public RateLimitFilter(RedisClient redisClient, ObjectMapper objectMapper) {
        this.redisClient = redisClient;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // 公开接口、认证接口、internal 接口、Swagger 不限流
        return path.startsWith("/api/v1/auth/")
                || path.startsWith("/api/v1/health")
                || path.startsWith("/api/internal/")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // userId 由 JWT 过滤器写入 request attribute
        Object userIdAttr = request.getAttribute("userId");
        if (userIdAttr == null) {
            // 未认证用户不限流（由 Spring Security 负责拦截）
            filterChain.doFilter(request, response);
            return;
        }

        Long userId = (Long) userIdAttr;
        String apiPath = request.getRequestURI();

        if (redisClient.isRateLimited(userId, apiPath, MAX_REQUESTS, WINDOW_SECONDS)) {
            log.warn("Rate limited: userId={}, path={}", userId, apiPath);
            response.setStatus(429);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            ApiResponse<Void> body = ApiResponse.fail(429, "请求频率过高，请稍后重试");
            response.getWriter().write(objectMapper.writeValueAsString(body));
            return;
        }

        filterChain.doFilter(request, response);
    }
}
