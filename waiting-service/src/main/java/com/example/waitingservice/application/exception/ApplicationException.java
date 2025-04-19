package com.example.waitingservice.application.exception;

public class ApplicationException extends RuntimeException {
    private ErrorCode code;

    public ErrorCode getCode() {
        return code;
    }
}
