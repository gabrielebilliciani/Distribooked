package it.unipi.distribooked.repository.mongo.custom;

import it.unipi.distribooked.dto.BooksByAgeGroupDTO;
import it.unipi.distribooked.model.embedded.EmbeddedBookRead;
import it.unipi.distribooked.model.embedded.EmbeddedBookSaved;
import org.bson.types.ObjectId;

import java.util.List;
import java.util.Map;

/**
 * Custom repository interface for User-related operations.
 */
public interface CustomUserRepository {
    /**
     * Removes a saved book for the user by its ID.
     *
     * @param userId The ID of the user.
     * @param bookId The ID of the book to remove.
     * @return True if the book was removed successfully, false otherwise.
     */
    boolean unsaveBook(String userId, String bookId);

    long saveBook(String userId, EmbeddedBookSaved bookToSave);

    boolean addReadBook(ObjectId userId, EmbeddedBookRead bookRead);

    List<BooksByAgeGroupDTO> findMostReadBooksByAgeGroup(String startDate, String endDate);

    Map<String, Map<String, Object>> findAverageAgeOfReadersByCity();

}
