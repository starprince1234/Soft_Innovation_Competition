package com.jigu.cloud.infrastructure.python.dto;

/**
 * 标记接口 — 所有 Python 内部调用请求的基类。
 * <p>
 * 具体请求请使用 DetectProcessRequest、DialogInternalRequest 等。
 * 内部 DTO 禁止复用对外 API DTO。
 */
public sealed interface PythonRequest
        permits DetectProcessRequest, DialogInternalRequest {
}
