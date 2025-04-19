package com.example.waitingservice.application.exception;

public enum ErrorCode {
    ALREADY_REGISTERED_USER("ALREADY_REGISTERED_USER", "User already registered"),
    ;

    private final String code;
    private final String description;

    ErrorCode(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
}
