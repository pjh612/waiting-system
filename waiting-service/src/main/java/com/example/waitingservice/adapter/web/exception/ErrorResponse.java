package com.example.waitingservice.adapter.web.exception;

public class ErrorResponse {
    private String code;
    private String desciption;

    public ErrorResponse(String code, String desciption) {
        this.code = code;
        this.desciption = desciption;
    }

    public String getCode() {
        return code;
    }

    public String getDesciption() {
        return desciption;
    }
}
