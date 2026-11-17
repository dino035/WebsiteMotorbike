package com.nguyen.motoparts.common.exception;

/** Gọi quá nhiều lần trong một khoảng thời gian (trả HTTP 429). */
public class TooManyRequestsException extends RuntimeException {
    public TooManyRequestsException(String message) {
        super(message);
    }
}
