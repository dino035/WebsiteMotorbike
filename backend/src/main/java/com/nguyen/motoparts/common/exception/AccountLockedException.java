package com.nguyen.motoparts.common.exception;

/** Tài khoản đang bị khóa → HTTP 403. */
public class AccountLockedException extends RuntimeException {
    public AccountLockedException(String message) {
        super(message);
    }
}
