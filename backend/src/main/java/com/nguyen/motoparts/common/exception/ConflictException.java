package com.nguyen.motoparts.common.exception;

/** Dữ liệu xung đột với trạng thái hiện có (vd: email đã đăng ký) → HTTP 409. */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
