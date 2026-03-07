package com.jigu.cloud.api.health;

import com.jigu.cloud.common.response.ApiResponse;
import com.jigu.cloud.infrastructure.bos.BosClient;
import com.jigu.cloud.infrastructure.python.PythonClient;
import com.jigu.cloud.infrastructure.redis.RedisClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 健康检查接口（permitAll，无需认证）。
 * <p>
 * 聚合 MySQL / Redis / BOS / Python 子服务状态。
 * 符合开发文档 4.7 节的响应格式规范。
 */
@RestController
@RequestMapping(path = "/api/v1/health", produces = MediaType.APPLICATION_JSON_VALUE)
public class HealthController {

    private final DataSource dataSource;
    private final RedisClient redisClient;
    private final BosClient bosClient;
    private final PythonClient pythonClient;

    public HealthController(DataSource dataSource,
                            RedisClient redisClient,
                            BosClient bosClient,
                            PythonClient pythonClient) {
        this.dataSource = dataSource;
        this.redisClient = redisClient;
        this.bosClient = bosClient;
        this.pythonClient = pythonClient;
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> check() {
        Map<String, Object> components = new LinkedHashMap<>();
        components.put("database", Map.of("status", checkMysql() ? "UP" : "DOWN"));
        components.put("redis", Map.of("status", redisClient.isHealthy() ? "UP" : "DOWN"));
        components.put("bos", Map.of("status", bosClient.isHealthy() ? "UP" : "DOWN"));

        // Python AI 服务（聚合检测模型、LLM、向量库等子依赖状态）
        boolean pythonHealthy = checkPython();
        components.put("ai_service_python", Map.of("status", pythonHealthy ? "UP" : "DOWN"));

        boolean dbOk = "UP".equals(((Map<?, ?>) components.get("database")).get("status"));
        boolean redisOk = "UP".equals(((Map<?, ?>) components.get("redis")).get("status"));

        String overallStatus;
        if (dbOk && redisOk && pythonHealthy) {
            overallStatus = "UP";
        } else if (dbOk && redisOk) {
            overallStatus = "PARTIAL";
        } else {
            overallStatus = "DOWN";
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", overallStatus);
        result.put("components", components);

        return ApiResponse.ok(result);
    }

    private boolean checkMysql() {
        try (Connection conn = dataSource.getConnection()) {
            return conn.isValid(3);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean checkPython() {
        try {
            return pythonClient.healthCheck();
        } catch (Exception e) {
            return false;
        }
    }
}
