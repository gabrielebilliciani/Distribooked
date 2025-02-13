package it.unipi.distribooked.exceptions;

public class BookSaveException extends BusinessException {
    public BookSaveException(String message) {
        super(message);
    }

    public static BookSaveException maxLimitReached() {
        return new BookSaveException("Maximum number of saved books (50) reached.");
    }

    public static BookSaveException alreadySaved() {
        return new BookSaveException("Book already saved in user's profile.");
    }
}