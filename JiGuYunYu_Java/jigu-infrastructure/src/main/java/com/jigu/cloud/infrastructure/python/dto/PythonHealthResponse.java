package com.jigu.cloud.infrastructure.python.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Python 健康检查响应（匹配 HealthCheckUseCase 返回的结构，
 * 从 InternalResponse.data 中解包后的数据）。
 */
public record PythonHealthResponse(
        @JsonProperty("healthy") Boolean healthy,
        @JsonProperty("dependencies") List<DependencyStatus> dependencies
) {

    public boolean isHealthy() {
        return Boolean.TRUE.equals(healthy);
    }

    public record DependencyStatus(
            @JsonProperty("name") String name,
            @JsonProperty("ok") Boolean ok,
            @JsonProperty("latency_ms") Float latencyMs,
            @JsonProperty("error") String error
    ) {
    }
}
