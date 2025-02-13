package it.unipi.distribooked.service;

import it.unipi.distribooked.dto.BookDTO;
import it.unipi.distribooked.exceptions.CannotRemoveBookException;
import it.unipi.distribooked.exceptions.LibraryEntryAlreadyExistsException;
import it.unipi.distribooked.exceptions.NoAvailableCopiesException;
import it.unipi.distribooked.mapper.AuthorMapper;
import it.unipi.distribooked.mapper.BookMapper;
import it.unipi.distribooked.model.Author;
import it.unipi.distribooked.model.Book;
import it.unipi.distribooked.model.enums.OutboxTaskType;
import it.unipi.distribooked.model.embedded.EmbeddedAuthor;
import it.unipi.distribooked.model.embedded.EmbeddedBookAuthor;
import it.unipi.distribooked.repository.mongo.AuthorRepository;
import it.unipi.distribooked.repository.mongo.BookRepository;
import it.unipi.distribooked.repository.mongo.LibraryRepository;
import it.unipi.distribooked.repository.mongo.views.EmbeddedAuthorView;
import it.unipi.distribooked.repository.redis.RedisBookRepository;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service class responsible for managing the book catalog.
 * Contains business logic for adding, updating, and removing books.
 */
@Service
public class CatalogueManagementService {

    private static final Logger logger = LoggerFactory.getLogger(CatalogueManagementService.class);

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private RedisBookRepository redisBookRepository;

    @Autowired
    private AuthorRepository authorRepository;

    @Autowired
    private LibraryRepository libraryRepository;

    @Autowired
    private OutboxService outboxService;

    @Autowired
    private BookMapper bookMapper;

    @Autowired
    private AuthorMapper authorMapper;

    /**
     * Adds a new book to the catalog and schedules author updates.
     *
     * @param bookDTO The book data to be added
     * @return BookDTO representing the saved book
     * @throws IllegalArgumentException if any authors don't exist
     */
    @Transactional // because we do not want to save the book without also scheduling the author updates
    public BookDTO addBook(BookDTO bookDTO) {
        logger.debug("Starting to process new book addition: {}", bookDTO.getTitle());

        // Validate book fields
        validateBookFields(bookDTO);

        // Verify all authors exist
        verifyAuthorsAndFields(bookDTO.getAuthors());

        // Create and save the book entity directly from DTO
        Book book = bookMapper.toBook(bookDTO);

        book = bookRepository.save(book);
        logger.info("Saved new book with ID: {}", book.getId());

        // Schedule author updates via outbox
        scheduleAuthorUpdates(book);

        return bookMapper.toBookDTO(book);
    }

    /**
     * Verifies that all authors exist.
     * Throws a single exception listing all missing authors if any don't exist.
     */
    private void verifyAuthorsAndFields(List<EmbeddedAuthor> authors) {
        // Ensure authors list is not null
        if (authors == null || authors.isEmpty()) {
            throw new IllegalArgumentException("At least one author must be provided.");
        }

        // Validate author IDs are not null
        authors.forEach(author -> {
            if (author.getId() == null) {
                throw new IllegalArgumentException("Author ID cannot be null");
            }
        });

        // Extract author IDs from the provided authors
        List<ObjectId> authorIds = authors.stream()
                .map(EmbeddedAuthor::getId)
                .collect(Collectors.toList());

        // Query the database for existing authors
        List<EmbeddedAuthorView> existingAuthors = authorRepository.findAuthorsByIdsWithFields(authorIds);

        // Collect IDs of existing authors into a Set for efficient lookup
        Set<ObjectId> existingIds = existingAuthors.stream()
                .map(EmbeddedAuthorView::getId)
                .collect(Collectors.toSet());

        // Find IDs that are missing in the database
        List<ObjectId> missingAuthorIds = authorIds.stream()
                .filter(id -> !existingIds.contains(id))
                .toList();

        // Throw an exception listing all missing author IDs
        if (!missingAuthorIds.isEmpty()) {
            throw new IllegalArgumentException(
                    String.format("The following authors do not exist in the database: %s",
                            missingAuthorIds.stream()
                                    .map(ObjectId::toHexString)
                                    .collect(Collectors.joining(", ")))
            );
        }

        // Validate existing authors have required fields
        for (EmbeddedAuthorView author : existingAuthors) {
            if (author.getFullName() == null || author.getFullName().isBlank()) {
                throw new IllegalArgumentException(
                        String.format("Author with ID %s has invalid data: missing fullName.", author.getId())
                );
            }
        }

        // Verify author names match database records
        for (EmbeddedAuthor providedAuthor : authors) {
            EmbeddedAuthorView dbAuthor = existingAuthors.stream()
                    .filter(a -> a.getId().equals(providedAuthor.getId()))
                    .findFirst()
                    .orElseThrow();

            if (!dbAuthor.getFullName().equals(providedAuthor.getFullName())) {
                throw new IllegalArgumentException(
                        String.format("Author name mismatch for ID %s: provided '%s' but found '%s' in database",
                                dbAuthor.getId(), providedAuthor.getFullName(), dbAuthor.getFullName())
                );
            }
        }
    }

    private void validateBookFields(BookDTO bookDTO) {
        // Check if the title field is provided
        if (bookDTO.getTitle() == null || bookDTO.getTitle().isBlank()) {
            throw new IllegalArgumentException("Book title is required.");
        }

        // Check if the branches field is provided (not allowed in this operation)
        if (bookDTO.getBranches() != null && !bookDTO.getBranches().isEmpty()) {
            throw new IllegalArgumentException("Books cannot have branches directly assigned. Use the dedicated endpoint.");
        }

        // additional field validations may be added
    }

    private void scheduleAuthorUpdates(Book book) {
        // Create the embedded book representation for authors
        EmbeddedBookAuthor embeddedBook = bookMapper.toEmbeddedBookAuthor(book);

        Map<String, Object> payload = new HashMap<>();
        payload.put("authorIds", book.getAuthors().stream()
                .map(author -> author.getId().toString())
                .collect(Collectors.toList()));
        payload.put("embeddedBook", embeddedBook);

        outboxService.createTask(OutboxTaskType.UPDATE_AUTHOR, payload);
        logger.debug("Scheduled author updates for book ID: {}", book.getId());
    }

    /**
     * Decrements the number of available copies of a book in a specific library.
     *
     * @param bookId    The ID of the book.
     * @param libraryId The ID of the library.
     */
    public void decrementCopiesInLibrary(String bookId, String libraryId) {
        try {
            redisBookRepository.decrementCopiesInLibrary(bookId, libraryId);
            logger.info("Successfully initiated copy decrement for book {} in library {}", bookId, libraryId);
        } catch (NoAvailableCopiesException e) {
            logger.warn("Cannot decrement copies for book {} in library {}: {}", bookId, libraryId, e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Error while decrementing copies for book {} in library {}: {}", bookId, libraryId, e.getMessage());
            throw new IllegalStateException("Failed to decrement book copies", e);
        }
    }

    /**
     * Removes a book entirely from a specific library.
     *
     * @param bookId    The ID of the book.
     * @param libraryId The ID of the library.
     */
    public void removeBookFromLibrary(String bookId, String libraryId) {
        ObjectId bookObjectId = new ObjectId(bookId);
        ObjectId libraryObjectId = new ObjectId(libraryId);

        // Fetch the total copies in MongoDB
        Integer totalCopiesInLibrary = bookRepository.getCopiesInLibrary(bookObjectId, libraryObjectId)
                .map(view -> view.getBranches().get(0).getNumberOfCopies())
                .orElseThrow(() -> new NoSuchElementException("Library or book not found"));

        // Attempt atomic removal in Redis and post to stream
        redisBookRepository.removeLibraryEntry(bookId, libraryId, totalCopiesInLibrary);

        logger.info("Successfully initiated removal of book {} from library {}", bookId, libraryId);
    }

    public void incrementCopiesInLibrary(String bookId, String libraryId) {
        try {
            redisBookRepository.incrementBookCopies(bookId, libraryId);
            logger.info("Requested increment of copies for book {} in library {}", bookId, libraryId);
        } catch (NoSuchElementException e) {
            logger.error("The library entry does not exist in Redis for book {} in library {}", bookId, libraryId);
            throw e;
        } catch (Exception e) {
            logger.error("Error while incrementing copies for book {} in library {}: {}",
                    bookId, libraryId, e.getMessage());
            throw e;
        }
    }

    public void addLibraryToBookAvailability(String bookId, String libraryId, Integer initialValue) {
        try {
            redisBookRepository.addLibraryToBookAvailability(bookId, libraryId, initialValue != null ? initialValue : 0);
            logger.info("Successfully initiated adding library {} to book {} availability", libraryId, bookId);
        } catch (LibraryEntryAlreadyExistsException e) {
            logger.warn("Cannot add library entry for book {} in library {}: {}", bookId, libraryId, e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Error while adding library entry for book {} in library {}: {}", bookId, libraryId, e.getMessage());
            throw new IllegalStateException("Failed to add library entry", e);
        }
    }

}
