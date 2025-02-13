package it.unipi.distribooked.service.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.unipi.distribooked.exceptions.TaskExecutionException;
import it.unipi.distribooked.model.OutboxTask;
import it.unipi.distribooked.model.embedded.EmbeddedBookAuthor;
import it.unipi.distribooked.repository.mongo.AuthorRepository;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Processor for UPDATE_AUTHOR tasks that updates the books array in author documents.
 */
@Component("UPDATE_AUTHOR")  // Name must match OutboxTaskType enum value
public class UpdateAuthorProcessor implements OutboxTaskProcessor {

    private static final Logger logger = LoggerFactory.getLogger(UpdateAuthorProcessor.class);

    @Autowired
    private AuthorRepository authorRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public void process(OutboxTask task) throws TaskExecutionException {
        logger.debug("Processing UPDATE_AUTHOR task: {}", task.getId());

        // Extract data from payload
        @SuppressWarnings("unchecked")  // this tag is needed because the payload is a Map<String, Object> and we know the types
                                        // without this tag, the compiler would give a warning
        List<String> authorIds = (List<String>) task.getPayload().get("authorIds");
        EmbeddedBookAuthor embeddedBook = objectMapper.convertValue(
                task.getPayload().get("embeddedBook"),
                EmbeddedBookAuthor.class
        );

        // Update each author document
        for (String authorId : authorIds) {
            updateAuthorDocument(new ObjectId(authorId), embeddedBook);
        }
    }

    /**
     * Updates a single author document by adding the book to their books array
     * and incrementing the totalBooks counter.
     */
    private void updateAuthorDocument(ObjectId authorId, EmbeddedBookAuthor book) {
        logger.debug("Updating author {} with book {}", authorId, book.getId());

        // Call the custom repository method to add the book atomically if it doesn't exist
        boolean updated = authorRepository.updateAuthorWithBook(authorId, book);

        // Log the outcome of the operation
        logger.debug("Author {} was {} with book {}",
                authorId,
                updated ? "updated" : "already up to date",
                book.getId());
    }

}