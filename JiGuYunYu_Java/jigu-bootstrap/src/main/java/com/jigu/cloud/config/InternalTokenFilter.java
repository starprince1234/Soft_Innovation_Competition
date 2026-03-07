package com.jigu.cloud.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jigu.cloud.common.response.ApiResponse;
import com.jigu.cloud.infrastructure.config.PythonProperties;
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
import java.security.MessageDigest;

/**
 * 内部接口鉴权过滤器。
 * <p>
 * 仅拦截 /api/internal/** 路径，校验请求头 X-Internal-Token
 * 是否与配置中的 jigu.python.internal-token 一致。
 * 使用常量时间比较防止时序攻击。
 */
public class InternalTokenFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(InternalTokenFilter.class);
    private static final String HEADER_INTERNAL_TOKEN = "X-Internal-Token";

    private final PythonProperties pythonProperties;
    private final ObjectMapper objectMapper;

    public InternalTokenFilter(PythonProperties pythonProperties, ObjectMapper objectMapper) {
        this.pythonProperties = pythonProperties;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // 只拦截 /api/internal/** 路径
        return !request.getRequestURI().startsWith("/api/internal");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String token = request.getHeader(HEADER_INTERNAL_TOKEN);
        String expected = pythonProperties.getInternalToken();

        if (token == null || expected == null || !constantTimeEquals(token, expected)) {
            log.warn("internal_auth_fail  path={} method={} reason=bad_token",
                    request.getRequestURI(), request.getMethod());
            writeError(response, 401, "Internal token invalid");
            return;
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 常量时间字符串比较，防止时序攻击。
     */
    private static boolean constantTimeEquals(String a, String b) {
        return MessageDigest.isEqual(
                a.getBytes(StandardCharsets.UTF_8),
                b.getBytes(StandardCharsets.UTF_8)
        );
    }

    private void writeError(HttpServletResponse response, int httpStatus, String message) throws IOException {
        response.setStatus(httpStatus);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        ApiResponse<Void> body = ApiResponse.fail(httpStatus, message);
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
