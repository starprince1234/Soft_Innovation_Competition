package com.jigu.cloud.api.admin;

import com.jigu.cloud.common.response.ApiResponse;
import com.jigu.cloud.infrastructure.python.PythonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

/**
 * 知识库管理接口（仅 MANAGER 角色可访问）。
 * <p>
 * 触发 Python 端 ChromaDB 向量重嵌入。
 */
@RestController
@RequestMapping(path = "/api/v1/admin/knowledge", produces = MediaType.APPLICATION_JSON_VALUE)
public class KnowledgeAdminController {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeAdminController.class);

    private final PythonClient pythonClient;

    public KnowledgeAdminController(PythonClient pythonClient) {
        this.pythonClient = pythonClient;
    }

    /**
     * 触发知识库向量重嵌入。
     * <p>
     * 异步通知 Python 端对所有已审核文物重新进行向量嵌入。
     * Python 端需实现 /internal/knowledge/reindex 接口。
     */
    @PostMapping("/reindex")
    public ApiResponse<Void> triggerReindex() {
        log.info("Knowledge reindex triggered by admin");
        pythonClient.triggerReindex();
        return ApiResponse.ok();
    }
}
