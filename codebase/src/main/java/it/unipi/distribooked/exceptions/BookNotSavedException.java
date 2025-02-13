package it.unipi.distribooked.exceptions;

public class BookNotSavedException extends BusinessException {
    public BookNotSavedException(String userId, String bookId) {
        super(String.format("Book %s not found in saved books for user %s", bookId, userId));
    }
}
