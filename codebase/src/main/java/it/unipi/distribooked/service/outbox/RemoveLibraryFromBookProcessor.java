package it.unipi.distribooked.service.outbox;

import it.unipi.distribooked.model.OutboxTask;
import it.unipi.distribooked.repository.mongo.BookRepository;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Processor for REMOVE_LIBRARY_FROM_BOOK tasks.
 */
@Slf4j
@Component("REMOVE_LIBRARY_FROM_BOOK")
public class RemoveLibraryFromBookProcessor implements OutboxTaskProcessor {

    @Autowired
    private BookRepository bookRepository;

    @Override
    public void process(OutboxTask task) {
        // Extract payload
        String bookId = (String) task.getPayload().get("bookId");
        String libraryId = (String) task.getPayload().get("libraryId");

        // Perform atomic remove
        int updatedCount = bookRepository.removeLibraryFromBookIfPresent(new ObjectId(bookId), new ObjectId(libraryId));

        if (updatedCount == 0) {
            throw new IllegalStateException("Cannot remove library: Library not present or already processed.");
        }

        log.info("Removed library {} from book {}", libraryId, bookId);
    }
}

