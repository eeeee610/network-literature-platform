package com.novel.platform.common.exception;

import org.springframework.http.HttpStatus;

/**
 * 业务异常：携带业务码与 HTTP 状态，由全局异常处理器转为统一响应。
 */
public class BusinessException extends RuntimeException {

    private final int code;
    private final HttpStatus status;

    public BusinessException(int code, HttpStatus status, String message) {
        super(message);
        this.code = code;
        this.status = status;
    }

    public int getCode() {
        return code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public static BusinessException badRequest(String message) {
        return new BusinessException(ErrorCodes.PARAM_INVALID, HttpStatus.BAD_REQUEST, message);
    }

    public static BusinessException unauthorized(String message) {
        return new BusinessException(ErrorCodes.UNAUTHORIZED, HttpStatus.UNAUTHORIZED, message);
    }

    public static BusinessException forbidden(String message) {
        return new BusinessException(ErrorCodes.FORBIDDEN, HttpStatus.FORBIDDEN, message);
    }

    public static BusinessException notFound(int code, String message) {
        return new BusinessException(code, HttpStatus.NOT_FOUND, message);
    }

    public static BusinessException conflict(int code, String message) {
        return new BusinessException(code, HttpStatus.CONFLICT, message);
    }
}
