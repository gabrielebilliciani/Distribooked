package it.unipi.distribooked.exceptions;

public class ConcurrentOperationException extends BusinessException {

    public ConcurrentOperationException(String message) {
        super(message);
    }
}
