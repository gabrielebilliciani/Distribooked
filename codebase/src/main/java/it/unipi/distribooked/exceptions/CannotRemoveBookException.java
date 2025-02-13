package it.unipi.distribooked.exceptions;

/**
 * Exception thrown when an attempt is made to remove a book from a library
 * but not all copies are available for removal.
 */
public class CannotRemoveBookException extends BusinessException {

    public CannotRemoveBookException(String message) {
        super(message);
    }
}
