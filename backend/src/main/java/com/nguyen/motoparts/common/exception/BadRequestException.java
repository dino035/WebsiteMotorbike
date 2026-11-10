package com.nguyen.motoparts.common.exception;

/** Yêu cầu không hợp lệ theo luật nghiệp vụ (vd: nhập sai, vượt giới hạn) → HTTP 400. */
public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}
