package com.jigu.cloud.common.enums;

/**
 * 统一错误码枚举。
 * <p>
 * 每个错误码绑定 HTTP 状态码和默认消息，确保与前端契约一致。
 * 新增错误码时必须标注触发场景，禁止"万能 500"。
 */
public enum ErrorCode {

    /* -------- 通用 -------- */
    SUCCESS(200, "OK"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未认证，请先登录"),
    FORBIDDEN(403, "无权限执行此操作"),
    NOT_FOUND(404, "资源不存在"),
    CONFLICT(409, "资源冲突"),
    TOO_MANY_REQUESTS(429, "请求频率过高，请稍后重试"),
    INTERNAL_ERROR(500, "服务器内部错误"),

    /* -------- 认证/鉴权 -------- */
    INVALID_CREDENTIALS(401, "用户名或密码错误"),
    TOKEN_EXPIRED(401, "登录已过期，请重新登录"),
    TOKEN_INVALID(401, "无效的访问令牌"),
    ACCOUNT_DISABLED(403, "账号已被禁用"),

    /* -------- 用户 -------- */
    USER_NOT_FOUND(404, "用户不存在"),
    USERNAME_EXISTS(409, "用户名已被注册"),

    /* -------- 文物 -------- */
    ARTIFACT_NOT_FOUND(404, "文物不存在"),
    ARTIFACT_NAME_EXISTS(409, "文物名称已存在"),

    /* -------- 检测 -------- */
    DETECT_TASK_NOT_FOUND(404, "检测任务不存在"),
    DETECT_IMAGE_TOO_LARGE(400, "图片大小超出限制"),
    DETECT_IMAGE_FORMAT_INVALID(400, "不支持的图片格式"),

    /* -------- 对话 -------- */
    DIALOG_NOT_FOUND(404, "对话记录不存在"),

    /* -------- 反馈 -------- */
    FEEDBACK_NOT_FOUND(404, "反馈记录不存在"),
    SCREENSHOT_TOO_LARGE(400, "截图大小超出限制"),

    /* -------- 下游依赖 -------- */
    PYTHON_SERVICE_UNAVAILABLE(503, "AI 服务暂时不可用"),
    BOS_UPLOAD_FAILED(502, "文件上传失败"),
    REDIS_UNAVAILABLE(503, "缓存服务暂时不可用");

    private final int httpStatus;
    private final String message;

    ErrorCode(int httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    public String getMessage() {
        return message;
    }
}
