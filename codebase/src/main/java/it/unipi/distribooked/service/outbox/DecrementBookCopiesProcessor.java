package it.unipi.distribooked.service.outbox;

import it.unipi.distribooked.model.OutboxTask;
import it.unipi.distribooked.repository.mongo.BookRepository;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Processor for DECREMENT_BOOK_COPIES tasks.
 */
@Slf4j
@Component("DECREMENT_BOOK_COPIES")
public class DecrementBookCopiesProcessor implements OutboxTaskProcessor {

    @Autowired
    private BookRepository bookRepository;

    @Override
    public void process(OutboxTask task) {
        // Extract payload
        String bookId = (String) task.getPayload().get("bookId");
        String libraryId = (String) task.getPayload().get("libraryId");

        // Perform atomic check and decrement
        int updatedCount = bookRepository.decrementCopiesInLibraryIfAvailable(new ObjectId(bookId), new ObjectId(libraryId));

        if (updatedCount == 0) {
            throw new IllegalStateException("Cannot decrement copies: No available copies to decrement or already processed.");
        }

        log.info("Decremented Mongo copies for book {} in library {}", bookId, libraryId);
    }
}


