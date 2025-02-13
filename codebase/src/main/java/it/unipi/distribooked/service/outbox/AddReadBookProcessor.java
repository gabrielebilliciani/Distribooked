package it.unipi.distribooked.service.outbox;

import it.unipi.distribooked.exceptions.TaskExecutionException;
import it.unipi.distribooked.model.OutboxTask;
import it.unipi.distribooked.model.Author;
import it.unipi.distribooked.model.embedded.EmbeddedAuthor;
import it.unipi.distribooked.model.embedded.EmbeddedBookRead;
import it.unipi.distribooked.repository.mongo.BookRepository;
import it.unipi.distribooked.repository.mongo.UserRepository;
import it.unipi.distribooked.repository.mongo.views.BookCatalogueView;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component("ADD_READ_BOOK")
public class AddReadBookProcessor implements OutboxTaskProcessor {

    private static final Logger logger = LoggerFactory.getLogger(AddReadBookProcessor.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookRepository bookRepository;

    @Override
    @Transactional
    public void process(OutboxTask task) throws TaskExecutionException {
        logger.debug("Processing ADD_READ_BOOK task: {}", task.getId());

        try {
            // Extract data from payload
            String userId = (String) task.getPayload().get("userId");
            String bookId = (String) task.getPayload().get("bookId");
            String libraryId = (String) task.getPayload().get("libraryId");
            String timestampStr = (String) task.getPayload().get("timestamp");

            // Parse timestamp from string
            long timestamp;
            try {
                timestamp = Long.parseLong(timestampStr);
            } catch (NumberFormatException e) {
                throw new TaskExecutionException("Invalid timestamp format: " + timestampStr, e);
            }

            // Convert IDs to ObjectId
            ObjectId userObjId = new ObjectId(userId);
            ObjectId bookObjId = new ObjectId(bookId);
            ObjectId libraryObjId = new ObjectId(libraryId);

            // Get book details
            Optional<BookCatalogueView> bookOptional = bookRepository.findBookById(bookObjId);
            if (bookOptional.isEmpty()) {
                throw new TaskExecutionException("Book not found: " + bookId);
            }
            BookCatalogueView book = bookOptional.get();

            // Create EmbeddedBookRead object
            EmbeddedBookRead bookRead = new EmbeddedBookRead();
            bookRead.setId(bookObjId);
            bookRead.setTitle(book.getTitle());

            // Convert EmbeddedAuthor list to Author list
            List<Author> authors = book.getAuthors().stream()
                    .map(embeddedAuthor -> {
                        Author author = new Author();
                        author.setId(embeddedAuthor.getId());
                        author.setFullName(embeddedAuthor.getFullName());
                        return author;
                    })
                    .collect(Collectors.toList());
            bookRead.setAuthors(authors);

            bookRead.setLibraryId(libraryObjId);
            bookRead.setReturnDate(LocalDateTime.ofInstant(
                    Instant.ofEpochSecond(timestamp),
                    ZoneOffset.UTC
            ));

            // Use repository to update user
            boolean updated = userRepository.addReadBook(userObjId, bookRead);
            if (!updated) {
                throw new TaskExecutionException("Failed to update user: " + userId);
            }

            // Increment readingsCount for the book
            long updatedDocs = bookRepository.incrementReadingsCount(bookObjId);
            if (updatedDocs == 0) {
                throw new TaskExecutionException("Failed to increment readings count for book: " + bookId);
            }

            logger.info("Successfully added read book {} to user {} and incremented readings count", bookId, userId);

        } catch (Exception e) {
            logger.error("Error processing ADD_READ_BOOK task: {}", e.getMessage());
            throw new TaskExecutionException("Failed to process ADD_READ_BOOK task", e);
        }
    }
}