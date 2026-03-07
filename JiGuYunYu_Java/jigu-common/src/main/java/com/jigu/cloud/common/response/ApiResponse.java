package com.jigu.cloud.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.jigu.cloud.common.enums.ErrorCode;

/**
 * 统一响应体 {@code {code, message, data}}。
 * <p>
 * 所有对外 API 必须返回此类型，禁止返回裸对象/Map 等。
 *
 * @param <T> data 载荷类型
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private final int code;
    private final String message;
    private final T data;

    private ApiResponse(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    /* -------- 成功 -------- */

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(200, "OK", data);
    }

    public static ApiResponse<Void> ok() {
        return new ApiResponse<>(200, "OK", null);
    }

    /* -------- 失败 -------- */

    public static <T> ApiResponse<T> fail(ErrorCode errorCode) {
        return new ApiResponse<>(errorCode.getHttpStatus(), errorCode.getMessage(), null);
    }

    public static <T> ApiResponse<T> fail(ErrorCode errorCode, String details) {
        return new ApiResponse<>(errorCode.getHttpStatus(), details, null);
    }

    public static <T> ApiResponse<T> fail(int code, String message) {
        return new ApiResponse<>(code, message, null);
    }

    /* -------- Getters -------- */

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public T getData() {
        return data;
    }
}
