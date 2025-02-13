package it.unipi.distribooked.service.outbox;

import it.unipi.distribooked.model.OutboxTask;
import it.unipi.distribooked.repository.mongo.BookRepository;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component("INCREMENT_BOOK_COPIES")
public class IncrementBookCopiesProcessor implements OutboxTaskProcessor {

    @Autowired
    private BookRepository bookRepository;

    @Override
    public void process(OutboxTask task) {
        String bookId = (String) task.getPayload().get("bookId");
        String libraryId = (String) task.getPayload().get("libraryId");

        bookRepository.incrementCopiesInLibrary(new ObjectId(bookId), new ObjectId(libraryId));
        log.info("Incremented Mongo copies for book {} in library {}", bookId, libraryId);
    }
}

