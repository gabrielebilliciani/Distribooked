package it.unipi.distribooked.repository.mongo.custom;

import it.unipi.distribooked.model.embedded.EmbeddedBookAuthor;
import org.bson.types.ObjectId;

/**
 * Custom repository interface for advanced Author operations.
 * Provides a method for atomically updating an author's embedded books.
 */
public interface CustomAuthorRepository {

    /**
     * Atomically adds a book to an author's embedded books if not already present.
     *
     * @param authorId The ID of the author to update.
     * @param book     The book to add to the embedded books.
     * @return true if the book was added, false if it was already present.
     */
    boolean updateAuthorWithBook(ObjectId authorId, EmbeddedBookAuthor book);
}
