package com.jigu.cloud.application.admin;

import com.jigu.cloud.common.enums.ErrorCode;
import com.jigu.cloud.common.exception.BizException;
import com.jigu.cloud.common.response.PageResponse;
import com.jigu.cloud.domain.user.User;
import com.jigu.cloud.domain.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 用户管理服务（仅 MANAGER 可访问）。
 */
@Service
public class UserAdminService {

    private static final Logger log = LoggerFactory.getLogger(UserAdminService.class);

    private final UserRepository userRepository;

    public UserAdminService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * 分页查询用户列表（支持用户名模糊搜索）。
     */
    public PageResponse<User> listUsers(String username, int page, int size,
                                          String sortBy, String direction) {
        List<User> list;
        long total;
        if (username != null && !username.isBlank()) {
            list = userRepository.findByUsernameLike(username, page, size, sortBy, direction);
            total = userRepository.countByUsernameLike(username);
        } else {
            list = userRepository.findAll(page, size, sortBy, direction);
            total = userRepository.count();
        }
        return PageResponse.of(list, total, page, size);
    }

    /**
     * 更新用户状态（ACTIVE / DISABLED）。
     */
    @Transactional
    public void updateUserStatus(Long userId, String status) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BizException(ErrorCode.USER_NOT_FOUND));
        user.setStatus(status);
        userRepository.save(user);
        log.info("User status updated: id={}, status={}", userId, status);
    }

    /**
     * 更新用户角色（PUBLIC / ARCHAEOLOGIST / MANAGER）。
     */
    @Transactional
    public void updateUserRole(Long userId, String role) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BizException(ErrorCode.USER_NOT_FOUND));
        user.setRole(role);
        userRepository.save(user);
        log.info("User role updated: id={}, role={}", userId, role);
    }
}
