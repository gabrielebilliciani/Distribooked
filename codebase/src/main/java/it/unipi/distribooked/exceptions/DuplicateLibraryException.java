package it.unipi.distribooked.exceptions;

/**
 * Exception thrown when attempting to add a duplicate library.
 */
public class DuplicateLibraryException extends BusinessException {
    public DuplicateLibraryException(String message) {
        super(message);
    }
}

