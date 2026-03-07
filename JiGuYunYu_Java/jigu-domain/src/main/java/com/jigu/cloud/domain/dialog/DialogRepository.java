package com.jigu.cloud.domain.dialog;

import java.util.List;
import java.util.Optional;

/**
 * 对话记录仓储接口。
 * <p>
 * "清空历史"必须以 userId 为约束条件，禁止无条件 delete。
 */
public interface DialogRepository {

    Optional<Dialog> findById(Long id);

    Dialog save(Dialog dialog);

    /** 按用户分页查询对话历史 */
    List<Dialog> findByUserId(Long userId, int page, int size);

    long countByUserId(Long userId);

    /** 获取用户对话的最大 turnId（用于生成下一轮） */
    int findMaxTurnIdByUserId(Long userId);

    /** 清空指定用户的所有历史记录（绑定 userId，禁止全表删除） */
    void deleteAllByUserId(Long userId);

    /** 按用户+文物维度删除对话历史 */
    void deleteAllByUserIdAndArtifactId(Long userId, Long artifactId);

    /** 分页查询所有对话历史（管理端） */
    List<Dialog> findAll(int page, int size);

    /** 按团队分页查询对话历史 */
    List<Dialog> findByTeamId(Long teamId, int page, int size);

    long countByTeamId(Long teamId);

    long count();
}
