package com.haufe.technical.api.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class ApiException extends Exception {
    private final HttpStatus code;

    public ApiException(HttpStatus code, String message) {
        super(message);
        this.code = code;
    }
}
