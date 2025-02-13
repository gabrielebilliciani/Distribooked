package it.unipi.distribooked.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.unipi.distribooked.dto.EmbeddedBookSavedDTO;
import it.unipi.distribooked.dto.ReservationDTO;
import it.unipi.distribooked.exceptions.ResourceNotFoundException;
import it.unipi.distribooked.mapper.ReservationMapper;
import it.unipi.distribooked.model.Reservation;
import it.unipi.distribooked.model.embedded.EmbeddedAuthor;
import it.unipi.distribooked.model.embedded.EmbeddedBookSaved;
import it.unipi.distribooked.repository.mongo.BookRepository;
import it.unipi.distribooked.repository.mongo.LibraryRepository;
import it.unipi.distribooked.repository.mongo.UserRepository;
import it.unipi.distribooked.repository.mongo.views.BookCatalogueView;
import it.unipi.distribooked.repository.redis.ReservationRepository;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;

/**
 * Service class responsible for handling reservations and saved books.
 * Contains business logic for managing user reservations and saved book lists.
 */
@Service
public class ReservationService {

    private static final Logger logger = LoggerFactory.getLogger(ReservationService.class);

    @Autowired
    private ReservationRepository redisReservationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ReservationMapper reservationMapper;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private LibraryRepository libraryRepository;

    /**
     * Reserves a book for a user.
     *
     * @param userId    The ID of the user making the reservation.
     * @param bookId    The ID of the book to reserve.
     * @param libraryId The ID of the library where the reservation is made.
     * @return A ReservationDTO containing the reservation details.
     */
    public ReservationDTO reserveBook(String userId, String bookId, String libraryId) {

        String rawBookTitle = bookRepository.findTitleById(bookId);
        String rawLibraryName = libraryRepository.findNameById(libraryId);

        String bookTitle = extractJsonValue(rawBookTitle, "title");
        String libraryName = extractJsonValue(rawLibraryName, "name");

//        logger.info("Found book: {}", bookTitle);
//        logger.info("Found library: {}", libraryName);

        if (bookTitle == null || libraryName == null) {
            throw new ResourceNotFoundException("Book or library not found");
        }

        // Delegate the reservation to the repository
        Reservation reservation = redisReservationRepository.reserveBook(
                userId,
                bookId,
                libraryId,
                bookTitle,
                libraryName
        );

        // convert the Reservation object to a ReservationDTO
        return reservationMapper.toReservationDTO(reservation);
    }

    private String extractJsonValue(String jsonString, String key) {
        if (jsonString == null || jsonString.isEmpty()) {
            return null;
        }
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(jsonString);
            return jsonNode.path(key).asText(null); // Ritorna il valore del campo o null se non trovato
        } catch (JsonProcessingException e) {
            logger.error("Error parsing JSON string: {}", jsonString, e);
            return null;
        }
    }


    /**
     * Cancels a reservation for a user in a specific library.
     *
     * @param userId    The ID of the user.
     * @param bookId    The ID of the book.
     * @param libraryId The ID of the library.
     * @throws IllegalStateException if the cancellation fails.
     */
    public void cancelReservation(String userId, String bookId, String libraryId) {
        boolean success = redisReservationRepository.cancelReservation(userId, bookId, libraryId);
        if (!success) {
            throw new IllegalStateException("Cancellation failed or reservation not found.");
        }
    }

    /**
     * Save a book for a user.
     *
     * @param userId The ID of the user.
     * @param bookId The ID of the book to save.
     * @return The saved book's details as an EmbeddedBookSavedDTO.
     */
    public EmbeddedBookSavedDTO saveBook(String userId, String bookId) {
        BookCatalogueView bookView = bookRepository.findBookById(new ObjectId(bookId))
                .orElseThrow(() -> new NoSuchElementException("Book not found with ID: " + bookId));

        logger.info("Found book: {}", bookView.getTitle());

        EmbeddedBookSaved savedBook = new EmbeddedBookSaved(
                bookView.getId(),
                bookView.getTitle(),
                bookView.getAuthors().stream()
                        .map(author -> new EmbeddedAuthor(new ObjectId(author.getId().toHexString()), author.getFullName()))
                        .toList()
        );

        long modifiedCount = userRepository.saveBook(userId, savedBook);

//        if (modifiedCount == 0) {
//            throw new IllegalStateException("Could not save book: limit reached or book already saved");
//        }

        return new EmbeddedBookSavedDTO(
                savedBook.getId().toHexString(),
                savedBook.getTitle(),
                savedBook.getAuthors()
        );
    }

    /**
     * Unsave a book for a user.
     */
    public void unsaveBook(String userId, String bookId) {
        userRepository.unsaveBook(userId, bookId);
    }
}
