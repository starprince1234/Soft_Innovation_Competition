package com.jigu.cloud.infrastructure.persistence.adapter;

import com.jigu.cloud.domain.dialog.Dialog;
import com.jigu.cloud.domain.dialog.DialogRepository;
import com.jigu.cloud.infrastructure.persistence.jpa.DialogJpaRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * DialogRepository 适配器。
 */
@Repository
public class DialogRepositoryImpl implements DialogRepository {

    private final DialogJpaRepository jpaRepository;

    public DialogRepositoryImpl(DialogJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<Dialog> findById(Long id) {
        return jpaRepository.findById(id);
    }

    @Override
    public Dialog save(Dialog dialog) {
        return jpaRepository.save(dialog);
    }

    @Override
    public List<Dialog> findByUserId(Long userId, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size);
        return jpaRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable).getContent();
    }

    @Override
    public long countByUserId(Long userId) {
        return jpaRepository.countByUserId(userId);
    }

    @Override
    public int findMaxTurnIdByUserId(Long userId) {
        return jpaRepository.findMaxTurnIdByUserId(userId);
    }

    @Override
    @Transactional
    public void deleteAllByUserId(Long userId) {
        jpaRepository.deleteAllByUserId(userId);
    }

    @Override
    @Transactional
    public void deleteAllByUserIdAndArtifactId(Long userId, Long artifactId) {
        jpaRepository.deleteAllByUserIdAndArtifactId(userId, artifactId);
    }

    @Override
    public List<Dialog> findAll(int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, org.springframework.data.domain.Sort.by(
                org.springframework.data.domain.Sort.Direction.DESC, "createdAt"));
        return jpaRepository.findAll(pageable).getContent();
    }

    @Override
    public long count() {
        return jpaRepository.count();
    }

    @Override
    public List<Dialog> findByTeamId(Long teamId, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size);
        return jpaRepository.findByTeamIdOrderByCreatedAtDesc(teamId, pageable).getContent();
    }

    @Override
    public long countByTeamId(Long teamId) {
        return jpaRepository.countByTeamId(teamId);
    }
}
