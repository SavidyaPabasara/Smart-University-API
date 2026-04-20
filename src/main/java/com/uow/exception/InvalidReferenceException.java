package com.uow.exception;

/**
 * Thrown when a POST /sensors body contains a roomId that does not exist.
 * Mapped to HTTP 422 Unprocessable Entity by InvalidReferenceMapper.
 */
public class InvalidReferenceException extends RuntimeException {
    public InvalidReferenceException(String message) {
        super(message);
    }
}
