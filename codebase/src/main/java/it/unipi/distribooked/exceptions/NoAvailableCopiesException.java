package it.unipi.distribooked.exceptions;

/**
 * Exception thrown when an attempt is made to decrement copies of a book
 * that are not available in the library.
 */
public class NoAvailableCopiesException extends BusinessException {

    public NoAvailableCopiesException(String message) {
        super(message);
    }
}
