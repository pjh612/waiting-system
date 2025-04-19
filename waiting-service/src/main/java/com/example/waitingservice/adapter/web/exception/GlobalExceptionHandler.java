package com.example.waitingservice.adapter.web.exception;

import com.example.waitingservice.application.exception.ApplicationException;
import com.example.waitingservice.application.exception.ErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import reactor.core.publisher.Mono;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApplicationException.class)
    public Mono<ResponseEntity<ErrorResponse>> onApplicationException(ApplicationException e) {
        ErrorCode code = e.getCode();

        return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(
                        code.getCode(),
                        code.getDescription()
                )));
    }
}
