package com.jigu.cloud.domain.artifact;

import java.util.List;
import java.util.Optional;

/**
 * 文物仓储接口。
 */
public interface ArtifactRepository {

    Optional<Artifact> findById(Long id);

    Optional<Artifact> findByName(String name);

    boolean existsByName(String name);

    Artifact save(Artifact artifact);

    void deleteById(Long id);

    /** 分页查询（可按状态、年代、名称、标签、关键词筛选） */
    List<Artifact> findByConditions(String status, String era, String name, String tags, String keyword,
                                     int page, int size, String sortBy, String direction);

    long countByConditions(String status, String era, String name, String tags, String keyword);

    /** 按状态分页查询（管理端） */
    List<Artifact> findByStatus(String status, int page, int size, String sortBy, String direction);

    long countByStatus(String status);

    long count();

    /** 查询所有指定状态的文物（不分页，知识库重建用） */
    List<Artifact> findAllByStatus(String status);
}
