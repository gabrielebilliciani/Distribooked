package it.unipi.distribooked.exceptions;

public class LibraryEntryAlreadyExistsException extends BusinessException {
    public LibraryEntryAlreadyExistsException(String bookId, String libraryId) {
        super(String.format("Library entry for book %s in library %s already exists", bookId, libraryId));
    }
}
