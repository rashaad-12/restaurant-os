package com.restaurantos.coresecurity.exception;

public class InvalidTokenException extends RuntimeException {

    public InvalidTokenException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidTokenException(String message) {
        super(message);
    }
}
