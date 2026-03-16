package com.jigu.cloud.infrastructure.persistence.adapter;

import com.jigu.cloud.domain.user.User;
import com.jigu.cloud.domain.user.UserRepository;
import com.jigu.cloud.infrastructure.persistence.jpa.UserJpaRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * UserRepository 适配器（domain 接口 → JPA 实现）。
 */
@Repository
public class UserRepositoryImpl implements UserRepository {

    private final UserJpaRepository jpaRepository;

    public UserRepositoryImpl(UserJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<User> findById(Long id) {
        return jpaRepository.findById(id);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return jpaRepository.findByUsername(username);
    }

    @Override
    public boolean existsByUsername(String username) {
        return jpaRepository.existsByUsername(username);
    }

    @Override
    public User save(User user) {
        return jpaRepository.save(user);
    }

    @Override
    public List<User> findAll(int page, int size, String sortBy, String direction) {
        PageRequest pageable = PageRequest.of(page, size, buildSort(sortBy, direction));
        return jpaRepository.findAll(pageable).getContent();
    }

    @Override
    public List<User> findByStatus(String status, int page, int size, String sortBy, String direction) {
        PageRequest pageable = PageRequest.of(page, size, buildSort(sortBy, direction));
        return jpaRepository.findByStatus(status, pageable).getContent();
    }

    @Override
    public List<User> findByUsernameLike(String username, int page, int size, String sortBy, String direction) {
        PageRequest pageable = PageRequest.of(page, size, buildSort(sortBy, direction));
        return jpaRepository.findByUsernameLike(username, pageable).getContent();
    }

    @Override
    public List<User> findByUsernameLikeAndStatus(String username, String status, int page, int size, String sortBy, String direction) {
        PageRequest pageable = PageRequest.of(page, size, buildSort(sortBy, direction));
        return jpaRepository.findByUsernameLikeAndStatus(username, status, pageable).getContent();
    }

    @Override
    public long countByUsernameLike(String username) {
        PageRequest pageable = PageRequest.of(0, 1);
        return jpaRepository.findByUsernameLike(username, pageable).getTotalElements();
    }

    @Override
    public long countByUsernameLikeAndStatus(String username, String status) {
        PageRequest pageable = PageRequest.of(0, 1);
        return jpaRepository.findByUsernameLikeAndStatus(username, status, pageable).getTotalElements();
    }

    @Override
    public long countByStatus(String status) {
        return jpaRepository.countByStatus(status);
    }

    @Override
    public long count() {
        return jpaRepository.count();
    }

    @Override
    public long countActiveUsers(int recentDays) {
        LocalDateTime since = LocalDateTime.now().minusDays(recentDays);
        return jpaRepository.countActiveUsersSince(since);
    }

    private Sort buildSort(String sortBy, String direction) {
        if (sortBy == null || sortBy.isBlank()) {
            sortBy = "createdAt";
        }
        Sort.Direction dir = "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return Sort.by(dir, sortBy);
    }
}
