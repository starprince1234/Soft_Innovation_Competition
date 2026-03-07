package com.jigu.cloud.application.detect;

import com.jigu.cloud.common.enums.ErrorCode;
import com.jigu.cloud.common.exception.BizException;
import com.jigu.cloud.domain.artifact.Artifact;
import com.jigu.cloud.domain.artifact.ArtifactRepository;
import com.jigu.cloud.domain.detect.DetectionTask;
import com.jigu.cloud.domain.detect.DetectionTaskRepository;
import com.jigu.cloud.infrastructure.bos.BosClient;
import com.jigu.cloud.infrastructure.python.PythonClient;
import com.jigu.cloud.infrastructure.python.dto.DetectProcessRequest;
import com.jigu.cloud.infrastructure.python.dto.DetectProcessResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 检测任务服务：创建任务、上传 BOS、异步调用 Python、查询状态/结果。
 * <p>
 * 状态机：PENDING → PROCESSING → COMPLETED / FAILED
 */
@Service
public class DetectService {

    private static final Logger log = LoggerFactory.getLogger(DetectService.class);

    private final DetectionTaskRepository taskRepository;
    private final BosClient bosClient;
    private final PythonClient pythonClient;
    private final ArtifactRepository artifactRepository;

    public DetectService(DetectionTaskRepository taskRepository,
                         BosClient bosClient,
                         PythonClient pythonClient,
                         ArtifactRepository artifactRepository) {
        this.taskRepository = taskRepository;
        this.bosClient = bosClient;
        this.pythonClient = pythonClient;
        this.artifactRepository = artifactRepository;
    }

    /**
     * 创建检测任务：接收 base64 → 上传 BOS → 入库 → 异步触发 Python。
     */
    @Transactional
    public DetectionTask createTask(Long userId, String imageBase64) {
        // 1. 上传到 BOS
        String imageUrl = bosClient.uploadBase64(imageBase64, "detect/", ".jpg");

        // 2. 创建任务
        DetectionTask task = new DetectionTask(userId, imageUrl);
        task = taskRepository.save(task);

        log.info("Detection task created: id={}, userId={}", task.getId(), userId);

        // 3. 异步触发 Python 检测（同时传递 base64 数据，BOS 不可达时 Python 仍可检测）
        asyncProcessDetection(task.getId(), imageUrl, imageBase64);

        return task;
    }

    /**
     * 异步调用 Python 检测。
     */
    @Async
    public void asyncProcessDetection(Long taskId, String imageUrl, String imageBase64) {
        try {
            // 更新状态为 PROCESSING
            DetectionTask task = taskRepository.findById(taskId)
                    .orElseThrow(() -> new BizException(ErrorCode.DETECT_TASK_NOT_FOUND));
            task.markProcessing();
            taskRepository.save(task);

            // 调用 Python（同时传递 imageUrl 和 imageBase64）
            DetectProcessResponse response = pythonClient.detectProcess(
                    new DetectProcessRequest(taskId, imageUrl, imageBase64));

            // 从 detections 数组中取最优结果
            if (response != null && response.detections() != null && !response.detections().isEmpty()) {
                // 按置信度降序，取最优
                var best = response.detections().stream()
                        .max((a, b) -> Float.compare(
                                a.confidence() != null ? a.confidence() : 0f,
                                b.confidence() != null ? b.confidence() : 0f))
                        .orElse(response.detections().get(0));

                String bboxStr = best.bbox() != null ? best.bbox().toString() : null;
                task.markCompleted(best.label(), best.confidence(), bboxStr, response.rawResult());
            } else {
                task.markFailed("No detections returned from model");
            }
            taskRepository.save(task);
            log.info("Detection task processed: id={}, status={}", taskId, task.getStatus());

        } catch (Exception e) {
            log.error("Detection task failed: id={}, error={}", taskId, e.getMessage(), e);
            taskRepository.findById(taskId).ifPresent(t -> {
                t.markFailed(e.getMessage());
                taskRepository.save(t);
            });
        }
    }

    /**
     * 查询任务状态（含所有权校验：仅任务所有者或 MANAGER 可访问）。
     */
    public DetectionTask getTaskStatus(Long taskId, Long userId, String role) {
        DetectionTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new BizException(ErrorCode.DETECT_TASK_NOT_FOUND));
        checkOwnership(task, userId, role);
        return task;
    }

    /**
     * 查询任务结果（含所有权校验：仅任务所有者或 MANAGER 可访问）。
     */
    public DetectionTask getTaskResult(Long taskId, Long userId, String role) {
        DetectionTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new BizException(ErrorCode.DETECT_TASK_NOT_FOUND));
        checkOwnership(task, userId, role);
        return task;
    }

    /**
     * 所有权校验：仅任务创建者本人或 MANAGER 角色允许访问。
     */
    private void checkOwnership(DetectionTask task, Long userId, String role) {
        if ("MANAGER".equals(role)) {
            return;
        }
        if (!task.getUserId().equals(userId)) {
            throw new BizException(ErrorCode.FORBIDDEN);
        }
    }

    /**
     * 根据检测结果的 detectedLabel 匹配文物库，返回匹配的 artifactId。
     * <p>
     * 仅匹配 APPROVED 状态的文物，未匹配到则返回 null。
     */
    public Long resolveArtifactId(String detectedLabel) {
        if (detectedLabel == null || detectedLabel.isBlank()) {
            return null;
        }
        return artifactRepository.findByName(detectedLabel)
                .filter(a -> "APPROVED".equals(a.getStatus()))
                .map(Artifact::getId)
                .orElse(null);
    }
}
