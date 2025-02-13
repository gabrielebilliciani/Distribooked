package it.unipi.distribooked.exceptions;

/**
 * Custom exception to indicate errors during task execution.
 */
public class TaskExecutionException extends Exception {

    public TaskExecutionException(String message) {
        super(message);
    }

    public TaskExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
}