package com.jigu.cloud.common.exception;

import com.jigu.cloud.common.enums.ErrorCode;

/**
 * 业务异常统一入口。
 * <p>
 * application 层遇到业务不可达状态统一 {@code throw new BizException(ErrorCode.xxx)}。
 * details 只放必要的可读信息，不放敏感数据（token、密码、密钥等）。
 */
public class BizException extends RuntimeException {

    private final ErrorCode errorCode;
    private final String details;

    public BizException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.details = null;
    }

    public BizException(ErrorCode errorCode, String details) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.details = details;
    }

    public BizException(ErrorCode errorCode, String details, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
        this.details = details;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public int getHttpStatus() {
        return errorCode.getHttpStatus();
    }

    public String getDetails() {
        return details;
    }
}
