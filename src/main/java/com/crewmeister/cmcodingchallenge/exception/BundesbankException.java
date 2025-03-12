package com.crewmeister.cmcodingchallenge.exception;

import org.springframework.http.HttpStatus;

public class BundesbankException extends RuntimeException {
    private final HttpStatus status;

    public BundesbankException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}

