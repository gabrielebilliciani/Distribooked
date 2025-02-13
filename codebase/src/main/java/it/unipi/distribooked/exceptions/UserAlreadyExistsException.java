package it.unipi.distribooked.exceptions;

/**
 * Custom exception to indicate that a user with the specified credentials already exists.
 *
 * This exception extends {@link RuntimeException} to provide unchecked exception handling,
 * making it suitable for scenarios where user duplication is encountered during operations like registration.
 */
public class UserAlreadyExistsException extends BusinessException {

    /**
     * Constructs a new UserAlreadyExistsException with the specified detail message.
     *
     * @param message The detail message, typically describing the cause of the exception.
     */
    public UserAlreadyExistsException(String message) {
        super(message); // Passes the message to the parent RuntimeException constructor
    }
}
