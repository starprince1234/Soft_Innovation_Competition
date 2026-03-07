package com.jigu.cloud.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jigu.cloud.common.response.ApiResponse;
import com.jigu.cloud.infrastructure.config.PythonProperties;
import com.jigu.cloud.infrastructure.redis.RedisClient;
import com.jigu.cloud.infrastructure.security.JwtTokenProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Spring Security 配置。
 * <p>
 * - 无状态会话（STATELESS），禁用 CSRF / httpBasic / formLogin
 * - JWT 认证通过自定义 OncePerRequestFilter 实现
 * - 黑名单校验通过 Redis 实现
 * - RBAC：/admin/** 仅 MANAGER 角色可访问
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    private final JwtTokenProvider jwtTokenProvider;
    private final RedisClient redisClient;
    private final ObjectMapper objectMapper;
    private final PythonProperties pythonProperties;

    public SecurityConfig(JwtTokenProvider jwtTokenProvider,
                          RedisClient redisClient,
                          ObjectMapper objectMapper,
                          PythonProperties pythonProperties) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.redisClient = redisClient;
        this.objectMapper = objectMapper;
        this.pythonProperties = pythonProperties;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // 公开接口
                        .requestMatchers("/api/v1/auth/register", "/api/v1/auth/login").permitAll()
                        .requestMatchers("/api/v1/health").permitAll()
                        .requestMatchers("/api/v1/artifacts", "/api/v1/artifact/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/feedback").permitAll()
                        // Swagger / OpenAPI
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
                        // 预检请求
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // Java 内部接口（供 Python 端调用，通过 InternalTokenFilter + X-Internal-Token 鉴权）
                        .requestMatchers("/api/internal/**").permitAll()
                        // 考古人员专属
                        .requestMatchers("/api/v1/archaeology/**").hasAnyAuthority("ARCHAEOLOGIST", "MANAGER")
                        // 管理后台仅 MANAGER
                        .requestMatchers("/api/v1/admin/**").hasAuthority("MANAGER")
                        // 其余接口需认证
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((req, resp, authEx) ->
                                writeError(resp, 401, "未认证，请先登录"))
                        .accessDeniedHandler((req, resp, accessDeniedEx) ->
                                writeError(resp, 403, "权限不足"))
                )
                .addFilterBefore(internalTokenFilter(), UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(rateLimitFilter(), AuthorizationFilter.class);

        return http.build();
    }

    /**
     * 内部接口 Token 过滤器。
     * <p>
     * 拦截 /api/internal/** 请求，校验 X-Internal-Token 头。
     * 非 internal 路径直接放行，不影响正常业务。
     */
    @Bean
    public InternalTokenFilter internalTokenFilter() {
        return new InternalTokenFilter(pythonProperties, objectMapper);
    }

    /**
     * API 限流过滤器。
     * <p>
     * 在 JWT 认证之后执行，基于 Redis 固定窗口计数器进行 per-user per-endpoint 限流。
     * Redis key: {@code api_rate_limit:{userId}:{apiPath}}
     */
    @Bean
    public RateLimitFilter rateLimitFilter() {
        return new RateLimitFilter(redisClient, objectMapper);
    }

    /**
     * JWT 认证过滤器。
     * <p>
     * 从 Authorization: Bearer {token} 中提取 Token，
     * 验证有效性 + 黑名单检查，通过后将 userId / role 写入 Request Attribute。
     */
    @Bean
    public OncePerRequestFilter jwtAuthenticationFilter() {
        return new OncePerRequestFilter() {

            @Override
            protected boolean shouldNotFilter(HttpServletRequest request) {
                String path = request.getRequestURI();
                return path.startsWith("/api/v1/auth/register")
                        || path.startsWith("/api/v1/auth/login")
                        || path.startsWith("/api/v1/health")
                        || path.startsWith("/swagger-ui")
                        || path.startsWith("/v3/api-docs");
            }

            @Override
            protected void doFilterInternal(HttpServletRequest request,
                                            HttpServletResponse response,
                                            FilterChain filterChain)
                    throws ServletException, IOException {

                String header = request.getHeader("Authorization");
                if (header == null || !header.startsWith("Bearer ")) {
                    filterChain.doFilter(request, response);
                    return;
                }

                String token = header.substring(7);

                // 1. 验证签名和有效期
                if (!jwtTokenProvider.validateToken(token)) {
                    writeError(response, 401, "Token 无效或已过期");
                    return;
                }

                // 2. 黑名单检查（已登出的 Token）
                String tokenHash = jwtTokenProvider.hashToken(token);
                if (redisClient.isBlacklisted(tokenHash)) {
                    writeError(response, 401, "Token 已被登出");
                    return;
                }

                // 3. 提取用户信息并写入 request attribute
                Long userId = jwtTokenProvider.getUserId(token);
                String role = jwtTokenProvider.getRole(token);
                String username = jwtTokenProvider.getUsername(token);

                request.setAttribute("userId", userId);
                request.setAttribute("role", role);
                request.setAttribute("username", username);
                request.setAttribute("token", token);

                // 4. 设置 Spring Security 上下文（用于 RBAC 鉴权）
                var authToken = new org.springframework.security.authentication
                        .UsernamePasswordAuthenticationToken(
                        username, null,
                        java.util.List.of(new org.springframework.security.core.authority
                                .SimpleGrantedAuthority(role))
                );
                org.springframework.security.core.context.SecurityContextHolder
                        .getContext().setAuthentication(authToken);

                filterChain.doFilter(request, response);
            }
        };
    }

    private void writeError(HttpServletResponse response, int httpStatus, String message) throws IOException {
        response.setStatus(httpStatus);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        ApiResponse<Void> body = ApiResponse.fail(httpStatus, message);
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
