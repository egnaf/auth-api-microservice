package com.github.egnaf.auth.exceptions;

import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;

@Getter
@Setter
public class InvalidDataException extends RuntimeException {

    private final String message;
    private final HttpStatus httpStatus;

    public InvalidDataException(String message, HttpStatus httpStatus) {
        this.message = message;
        this.httpStatus = httpStatus;
    }
}
