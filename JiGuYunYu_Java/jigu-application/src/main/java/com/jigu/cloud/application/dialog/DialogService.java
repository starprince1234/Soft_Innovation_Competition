package com.jigu.cloud.application.dialog;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jigu.cloud.common.enums.ErrorCode;
import com.jigu.cloud.common.exception.BizException;
import com.jigu.cloud.common.response.PageResponse;
import com.jigu.cloud.domain.dialog.Dialog;
import com.jigu.cloud.domain.dialog.DialogRepository;
import com.jigu.cloud.infrastructure.python.PythonClient;
import com.jigu.cloud.infrastructure.python.dto.DialogInternalRequest;
import com.jigu.cloud.infrastructure.python.dto.DialogInternalResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * 对话服务：发起对话、获取结果、历史查询、清空历史。
 * <p>
 * 清空历史必须绑定 userId（禁止全表删除）。
 * 调用 Python 对话接口必须通过 PythonClient（内部鉴权）。
 */
@Service
public class DialogService {

    private static final Logger log = LoggerFactory.getLogger(DialogService.class);

    private final DialogRepository dialogRepository;
    private final PythonClient pythonClient;
    private final ObjectMapper objectMapper;

    public DialogService(DialogRepository dialogRepository, PythonClient pythonClient,
                         ObjectMapper objectMapper) {
        this.dialogRepository = dialogRepository;
        this.pythonClient = pythonClient;
        this.objectMapper = objectMapper;
    }

    /**
     * 发起对话请求并获取 AI 回复。
     */
    @Transactional
    public Dialog createDialog(Long userId, String userRole, String userQuery, Long artifactId,
                               List<DialogInternalRequest.ContextTurn> contextHistory) {
        // 获取下一轮 turnId
        int nextTurn = dialogRepository.findMaxTurnIdByUserId(userId) + 1;

        // 构建对话任务 ID
        String dialogTaskId = "dialog_" + userId + "_" + nextTurn + "_" + System.currentTimeMillis();

        // 调用 Python 对话接口（传递所有必要字段）
        DialogInternalRequest request = new DialogInternalRequest(
                dialogTaskId,
                userId,
                userRole,
                userQuery,
                artifactId,
                contextHistory,
                3,    // top_k 默认值
                0.7f  // 本地 RAG 置信度阈值
        );
        DialogInternalResponse response = pythonClient.dialog(request);

        // 保存对话记录
        Dialog dialog = new Dialog(userId, artifactId, nextTurn, userQuery, response.answer());

        // ---- rag_sources：结构化 JSON 存储 ----
        if (response.sources() != null && !response.sources().isEmpty()) {
            try {
                // 将 sources 序列化为 JSON 数组字符串
                List<Map<String, Object>> sourceMaps = response.sources().stream()
                        .map(s -> Map.<String, Object>of(
                                "title", s.title() != null ? s.title() : "",
                                "url", s.url() != null ? s.url() : "",
                                "score", s.score() != null ? s.score() : 0f,
                                "chunk_id", s.chunkId() != null ? s.chunkId() : ""
                        ))
                        .toList();
                dialog.setRagSources(objectMapper.writeValueAsString(sourceMaps));
            } catch (JsonProcessingException e) {
                log.warn("Failed to serialize rag_sources to JSON, fallback to text: {}", e.getMessage());
                // 回退到简单文本
                StringBuilder sb = new StringBuilder();
                for (var source : response.sources()) {
                    if (!sb.isEmpty()) sb.append(" | ");
                    sb.append(source.title());
                }
                dialog.setRagSources(sb.toString());
            }
        }

        // ---- context_snapshot：保存当轮对话上下文快照 ----
        if (contextHistory != null && !contextHistory.isEmpty()) {
            try {
                dialog.setContextSnapshot(objectMapper.writeValueAsString(contextHistory));
            } catch (JsonProcessingException e) {
                log.warn("Failed to serialize context_snapshot: {}", e.getMessage());
            }
        }

        dialog = dialogRepository.save(dialog);

        log.info("Dialog created: id={}, userId={}, turnId={}, model={}, sources={}",
                dialog.getId(), userId, nextTurn, response.model(),
                response.sources() != null ? response.sources().size() : 0);
        return dialog;
    }

    /**
     * 获取对话结果（含所有权校验：仅对话所有者或 MANAGER 可访问）。
     */
    public Dialog getById(Long id, Long userId, String role) {
        Dialog dialog = dialogRepository.findById(id)
                .orElseThrow(() -> new BizException(ErrorCode.DIALOG_NOT_FOUND));
        if (!"MANAGER".equals(role) && !dialog.getUserId().equals(userId)) {
            throw new BizException(ErrorCode.FORBIDDEN);
        }
        return dialog;
    }

    /**
     * 查询用户对话历史。
     */
    public PageResponse<Dialog> getHistory(Long userId, int page, int size) {
        List<Dialog> list = dialogRepository.findByUserId(userId, page, size);
        long total = dialogRepository.countByUserId(userId);
        return PageResponse.of(list, total, page, size);
    }

    /**
     * 清空用户对话历史（绑定 userId，禁止全表删除）。
     * 支持按 artifactId 选择性删除。
     */
    @Transactional
    public void clearHistory(Long userId, Long artifactId) {
        if (artifactId != null) {
            dialogRepository.deleteAllByUserIdAndArtifactId(userId, artifactId);
            log.info("Dialog history cleared for userId={}, artifactId={}", userId, artifactId);
        } else {
            dialogRepository.deleteAllByUserId(userId);
            log.info("Dialog history cleared for userId={}", userId);
        }
    }

    /**
     * 查询所有对话历史（管理端）。
     */
    public PageResponse<Dialog> getAllHistory(int page, int size) {
        List<Dialog> list = dialogRepository.findAll(page, size);
        long total = dialogRepository.count();
        return PageResponse.of(list, total, page, size);
    }

    /**
     * 查询团队对话历史（按 team_id 过滤）。
     */
    public PageResponse<Dialog> getTeamHistory(Long teamId, int page, int size) {
        List<Dialog> list = dialogRepository.findByTeamId(teamId, page, size);
        long total = dialogRepository.countByTeamId(teamId);
        return PageResponse.of(list, total, page, size);
    }
}
