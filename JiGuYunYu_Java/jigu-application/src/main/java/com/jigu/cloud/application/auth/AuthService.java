package com.jigu.cloud.application.auth;

import com.jigu.cloud.common.enums.ErrorCode;
import com.jigu.cloud.common.exception.BizException;
import com.jigu.cloud.domain.user.User;
import com.jigu.cloud.domain.user.UserRepository;
import com.jigu.cloud.infrastructure.redis.RedisClient;
import com.jigu.cloud.infrastructure.security.JwtTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 认证服务：注册、登录、登出。
 * <p>
 * 密码只存 hash，不允许明文。登出必须写 Redis JWT 黑名单。
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final RedisClient redisClient;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository,
                       JwtTokenProvider jwtTokenProvider,
                       RedisClient redisClient) {
        this.userRepository = userRepository;
        this.jwtTokenProvider = jwtTokenProvider;
        this.redisClient = redisClient;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    /**
     * 用户注册。
     */
    @Transactional
    public Long register(String username, String password) {
        if (userRepository.existsByUsername(username)) {
            throw new BizException(ErrorCode.USERNAME_EXISTS);
        }
        String hash = passwordEncoder.encode(password);
        User user = new User(username, hash, "PUBLIC", "ACTIVE");
        User saved = userRepository.save(user);
        log.info("User registered: id={}, username={}", saved.getId(), saved.getUsername());
        return saved.getId();
    }

    /**
     * 用户登录 — 返回 LoginResult（application 层内部对象）。
     */
    @Transactional
    public LoginResult login(String username, String password) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BizException(ErrorCode.INVALID_CREDENTIALS));

        if (!user.isActive()) {
            throw new BizException(ErrorCode.ACCOUNT_DISABLED);
        }

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new BizException(ErrorCode.INVALID_CREDENTIALS);
        }

        // 更新最后登录时间
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        // 生成 Token
        String token = jwtTokenProvider.generateToken(user.getId(), user.getUsername(), user.getRole());
        long expiresIn = jwtTokenProvider.getExpiresSeconds();

        log.info("User logged in: id={}, username={}", user.getId(), user.getUsername());
        return new LoginResult(token, expiresIn, List.of(user.getRole()));
    }

    /**
     * 用户登出 — 将 Token 加入 Redis 黑名单。
     */
    public void logout(String token) {
        String tokenHash = jwtTokenProvider.hashToken(token);
        long remainingSeconds = jwtTokenProvider.getRemainingSeconds(token);
        if (remainingSeconds > 0) {
            redisClient.addToBlacklist(tokenHash, remainingSeconds);
            log.info("User logged out, token blacklisted, ttl={}s", remainingSeconds);
        }
    }

    /**
     * 获取当前登录用户信息。
     */
    public User getCurrentUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BizException(ErrorCode.USER_NOT_FOUND));
    }

    /**
     * 修改当前用户密码。
     */
    @Transactional
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BizException(ErrorCode.USER_NOT_FOUND));
        if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            throw new BizException(ErrorCode.INVALID_CREDENTIALS, "旧密码不正确");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        log.info("User password changed: id={}", userId);
    }

    /**
     * 登录结果内部传输对象（不暴露给 API 层，由 Controller 转换为 LoginResponse）。
     */
    public record LoginResult(String token, long expiresIn, List<String> roles) {
    }
}
