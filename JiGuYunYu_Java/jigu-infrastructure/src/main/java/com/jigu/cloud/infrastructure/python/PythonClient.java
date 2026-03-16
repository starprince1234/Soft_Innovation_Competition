package com.jigu.cloud.infrastructure.python;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jigu.cloud.common.enums.ErrorCode;
import com.jigu.cloud.common.exception.BizException;
import com.jigu.cloud.infrastructure.config.PythonProperties;
import com.jigu.cloud.infrastructure.python.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * Java 调用 Python 内部接口的统一入口。
 * <p>
 * 所有请求统一加 X-Internal-Token。统一超时、错误码映射、日志埋点。
 * <p>
 * Python 内部接口统一返回 {@code {"code": 200, "message": "Success", "data": {...}}} 格式（InternalResponse 包装），
 * 本客户端负责拆包 data 节点并反序列化为目标 DTO。
 * <p>
 * <strong>严禁在 application/controller 中自行拼 URL 调 Python。</strong>
 */
@Component
public class PythonClient {

    private static final Logger log = LoggerFactory.getLogger(PythonClient.class);
    private static final String HEADER_INTERNAL_TOKEN = "X-Internal-Token";

    private final PythonProperties pythonProperties;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public PythonClient(PythonProperties pythonProperties,
                        RestTemplateBuilder builder,
                        ObjectMapper objectMapper) {
        this.pythonProperties = pythonProperties;
        this.objectMapper = objectMapper;
        this.restTemplate = builder
                .setConnectTimeout(Duration.ofMillis(pythonProperties.getConnectTimeout()))
                .setReadTimeout(Duration.ofMillis(pythonProperties.getReadTimeout()))
                .build();
    }

    /**
     * 调用 Python 检测接口：/internal/detect/process
     */
    public DetectProcessResponse detectProcess(DetectProcessRequest request) {
        log.info("PythonClient.detectProcess taskId={}", request.taskId());
        return callInternal("/internal/detect/process", HttpMethod.POST, request, DetectProcessResponse.class);
    }

    /**
     * 调用 Python 对话接口：/internal/dialog/responses
     */
    public DialogInternalResponse dialog(DialogInternalRequest request) {
        log.info("PythonClient.dialog query length={}", request.query().length());
        return callInternal("/internal/dialog/responses", HttpMethod.POST, request, DialogInternalResponse.class);
    }

    /**
     * 调用 Python 健康检查接口：/internal/health
     */
    public boolean healthCheck() {
        try {
            PythonHealthResponse resp = callInternal("/internal/health", HttpMethod.GET, null, PythonHealthResponse.class);
            return resp != null && resp.isHealthy();
        } catch (Exception e) {
            log.warn("Python health check failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 触发知识库向量重嵌入：/internal/knowledge/reindex
     * <p>
     * 通知 Python 端对所有已审核文物重新进行向量嵌入（ChromaDB）。
     */
    public void triggerReindex() {
        log.info("PythonClient.triggerReindex called");
        callInternal("/internal/knowledge/reindex", HttpMethod.POST, null, JsonNode.class);
    }

    /**
     * 单文物向量 upsert：/internal/knowledge/upsert
     * <p>
     * 文物 APPROVED 或文物信息更新时调用，将该文物的语义向量写入 ChromaDB。
     * 设计为 fire-and-forget，调用方捕获异常避免影响主流程。
     *
     * @param artifactId  文物 ID
     * @param name        文物名称
     * @param description 描述
     * @param era         年代
     * @param location    产地
     * @param tags        标签
     */
    public void upsertArtifactVector(Long artifactId, String name, String description,
                                     String era, String location, String tags) {
        log.info("PythonClient.upsertArtifactVector artifactId={}", artifactId);
        var body = java.util.Map.of(
                "artifactId", artifactId,
                "name", name != null ? name : "",
                "description", description != null ? description : "",
                "era", era != null ? era : "",
                "location", location != null ? location : "",
                "tags", tags != null ? tags : ""
        );
        callInternal("/internal/knowledge/upsert", HttpMethod.POST, body, JsonNode.class);
    }

    /**
     * 删除文物向量：/internal/knowledge/delete
     * <p>
     * 文物被删除时调用，从 ChromaDB 移除对应的向量。
     * 设计为 fire-and-forget，调用方捕获异常避免影响主流程。
     *
     * @param artifactId 文物 ID
     */
    public void deleteArtifactVector(Long artifactId) {
        log.info("PythonClient.deleteArtifactVector artifactId={}", artifactId);
        var body = java.util.Map.of("artifactId", artifactId);
        callInternal("/internal/knowledge/delete", HttpMethod.POST, body, JsonNode.class);
    }

    // -------- 核心调用方法：拆包 InternalResponse --------

    /**
     * 通用内部调用方法。
     * <p>
     * 流程：发送请求 → 接收原始 JSON 字符串 → 解析 InternalResponse 包装
     * → 检查 code → 提取 data 节点 → 反序列化为目标类型。
     *
     * @param path         接口路径（如 /internal/detect/process）
     * @param method       HTTP 方法
     * @param body         请求体（可为 null）
     * @param responseType data 节点的目标类型
     * @param <T>          返回类型
     * @return 反序列化后的 data 对象
     */
    private <T> T callInternal(String path, HttpMethod method, Object body, Class<T> responseType) {
        String url = pythonProperties.getBaseUrl() + path;
        long start = System.currentTimeMillis();

        try {
            ResponseEntity<String> rawResp = restTemplate.exchange(
                    url, method, buildEntity(body), String.class);

            long costMs = System.currentTimeMillis() - start;
            log.info("PythonClient call completed  path={} cost={}ms status={}",
                    path, costMs, rawResp.getStatusCode());

            String rawBody = rawResp.getBody();
            if (rawBody == null || rawBody.isBlank()) {
                return null;
            }

            // 解析顶层 InternalResponse 结构
            JsonNode root = objectMapper.readTree(rawBody);
            int code = root.has("code") ? root.get("code").asInt() : 200;
            boolean success = (code == 200 || code == 0);

            if (!success) {
                String message = root.has("message") ? root.get("message").asText() : "Unknown error";
                log.error("Python internal API error  path={} code={} message={}", path, code, message);
                throw new BizException(ErrorCode.PYTHON_SERVICE_UNAVAILABLE,
                        String.format("Python 服务返回错误: code=%d, message=%s", code, message));
            }

            // 提取 data 节点并反序列化为目标类型
            JsonNode dataNode = root.get("data");
            if (dataNode == null || dataNode.isNull()) {
                return null;
            }

            return objectMapper.treeToValue(dataNode, responseType);

        } catch (RestClientException e) {
            long costMs = System.currentTimeMillis() - start;
            log.error("PythonClient call failed  path={} cost={}ms error={}", path, costMs, e.getMessage());
            throw new BizException(ErrorCode.PYTHON_SERVICE_UNAVAILABLE,
                    String.format("%s 服务调用失败: %s", path, e.getMessage()));
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            long costMs = System.currentTimeMillis() - start;
            log.error("PythonClient response parsing failed  path={} cost={}ms error={}", path, costMs, e.getMessage());
            throw new BizException(ErrorCode.PYTHON_SERVICE_UNAVAILABLE,
                    String.format("%s 响应解析失败: %s", path, e.getMessage()));
        }
    }

    // -------- 内部方法 --------

    private <T> HttpEntity<T> buildEntity(T body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(HEADER_INTERNAL_TOKEN, pythonProperties.getInternalToken());
        return new HttpEntity<>(body, headers);
    }
}
