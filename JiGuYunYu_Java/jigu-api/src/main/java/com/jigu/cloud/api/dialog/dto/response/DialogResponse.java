package com.jigu.cloud.api.dialog.dto.response;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jigu.cloud.domain.dialog.Dialog;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 对话响应 VO。
 * <p>
 * rag_sources 在数据库中以 JSON 数组存储（由 DialogService 写入），
 * 此处解析为结构化列表返回给前端。
 */
public record DialogResponse(
        Long dialogId,
        Integer turnId,
        String userQuery,
        String aiResponse,
        List<Map<String, Object>> ragSources,
        LocalDateTime createdAt
) {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static DialogResponse from(Dialog d) {
        List<Map<String, Object>> sources = parseRagSources(d.getRagSources());
        return new DialogResponse(
                d.getId(), d.getTurnId(), d.getUserQuery(),
                d.getAiResponse(), sources, d.getCreatedAt()
        );
    }

    /**
     * 解析 rag_sources 字段（兼容 JSON 数组和旧式逗号分隔格式）。
     */
    private static List<Map<String, Object>> parseRagSources(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }

        String trimmed = raw.strip();

        // 优先尝试 JSON 数组解析（新格式）
        if (trimmed.startsWith("[")) {
            try {
                return MAPPER.readValue(trimmed, new TypeReference<>() {});
            } catch (JsonProcessingException ignored) {
                // JSON 解析失败，回退到旧格式
            }
        }

        // 旧格式兼容：逗号或管道分隔的纯文本 → 转为 [{title: x}] 结构
        String[] parts = trimmed.contains("|") ? trimmed.split("\\s*\\|\\s*") : trimmed.split(",");
        return java.util.Arrays.stream(parts)
                .filter(s -> !s.isBlank())
                .map(s -> Map.<String, Object>of("title", s.strip()))
                .toList();
    }
}
