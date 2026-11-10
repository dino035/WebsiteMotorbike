package com.nguyen.motoparts.common.exception;

/** Không gửi được email (lỗi dịch vụ bên ngoài) → HTTP 503. */
public class EmailSendException extends RuntimeException {
    public EmailSendException(String message, Throwable cause) {
        super(message, cause);
    }
}
