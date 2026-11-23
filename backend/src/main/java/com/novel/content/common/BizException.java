package com.novel.content.common;

import org.springframework.http.HttpStatus;

public class BizException extends RuntimeException {

    private final int code;
    private final HttpStatus httpStatus;

    public BizException(int code, String message, HttpStatus httpStatus) {
        super(message);
        this.code = code;
        this.httpStatus = httpStatus;
    }

    public static BizException badRequest(String message) {
        return new BizException(40001, message, HttpStatus.BAD_REQUEST);
    }

    public static BizException unauthorized(String message) {
        return new BizException(40101, message, HttpStatus.UNAUTHORIZED);
    }

    public static BizException forbidden(String message) {
        return new BizException(40301, message, HttpStatus.FORBIDDEN);
    }

    public static BizException notFound(String message) {
        return new BizException(40401, message, HttpStatus.NOT_FOUND);
    }

    public int getCode() {
        return code;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
