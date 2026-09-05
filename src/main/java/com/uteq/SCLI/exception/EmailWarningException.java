package com.uteq.SCLI.exception;

public class EmailWarningException extends RuntimeException {
    public EmailWarningException(String message, Throwable cause) {
        super(message, cause);
    }
}