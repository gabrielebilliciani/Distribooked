package it.unipi.distribooked.exceptions;

public class InvalidPayloadException extends RuntimeException {
    public InvalidPayloadException(String message, Throwable cause) {
        super(message, cause);
    }
}
