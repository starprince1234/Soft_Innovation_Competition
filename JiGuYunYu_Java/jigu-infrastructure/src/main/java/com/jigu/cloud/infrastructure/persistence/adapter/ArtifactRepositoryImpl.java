package com.jigu.cloud.infrastructure.persistence.adapter;

import com.jigu.cloud.domain.artifact.Artifact;
import com.jigu.cloud.domain.artifact.ArtifactRepository;
import com.jigu.cloud.infrastructure.persistence.jpa.ArtifactJpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * ArtifactRepository 适配器。
 */
@Repository
public class ArtifactRepositoryImpl implements ArtifactRepository {

    private final ArtifactJpaRepository jpaRepository;

    public ArtifactRepositoryImpl(ArtifactJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<Artifact> findById(Long id) {
        return jpaRepository.findByIdAndDeletedFalse(id);
    }

    @Override
    public Optional<Artifact> findByName(String name) {
        return jpaRepository.findByNameAndDeletedFalse(name);
    }

    @Override
    public boolean existsByName(String name) {
        return jpaRepository.existsByNameAndDeletedFalse(name);
    }

    @Override
    public boolean existsByNameIncludingDeleted(String name) {
        return jpaRepository.existsByName(name);
    }

    @Override
    public Artifact save(Artifact artifact) {
        return jpaRepository.save(artifact);
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        jpaRepository.softDeleteById(id);
    }

    @Override
    public List<Artifact> findByConditions(String status, String era, String name, String tags, String keyword,
                                           int page, int size, String sortBy, String direction) {
        Sort sort = buildSort(sortBy, direction);
        PageRequest pageable = PageRequest.of(page, size, sort);
        Page<Artifact> result = jpaRepository.findByConditions(status, era, name, tags, keyword, pageable);
        return result.getContent();
    }

    @Override
    public long countByConditions(String status, String era, String name, String tags, String keyword) {
        PageRequest pageable = PageRequest.of(0, 1);
        return jpaRepository.findByConditions(status, era, name, tags, keyword, pageable).getTotalElements();
    }

    @Override
    public List<Artifact> findByStatus(String status, int page, int size, String sortBy, String direction) {
        PageRequest pageable = PageRequest.of(page, size, buildSort(sortBy, direction));
        return jpaRepository.findByStatusAndDeletedFalse(status, pageable).getContent();
    }

    @Override
    public long countByStatus(String status) {
        return jpaRepository.countByStatus(status);
    }

    @Override
    public long countByStatusAndCreatedAfter(String status, LocalDateTime since) {
        return jpaRepository.countByStatusAndCreatedAtAfter(status, since);
    }

    @Override
    public long count() {
        return jpaRepository.countActiveArtifacts();
    }

    @Override
    public List<Artifact> findAllByStatus(String status) {
        return jpaRepository.findAllByStatusAndDeletedFalse(status);
    }

    private Sort buildSort(String sortBy, String direction) {
        if (sortBy == null || sortBy.isBlank()) {
            sortBy = "createdAt";
        }
        Sort.Direction dir = "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return Sort.by(dir, sortBy);
    }
}
