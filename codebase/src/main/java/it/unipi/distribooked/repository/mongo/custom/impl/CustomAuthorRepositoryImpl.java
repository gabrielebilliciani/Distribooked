package it.unipi.distribooked.repository.mongo.custom.impl;

import com.mongodb.client.result.UpdateResult;
import it.unipi.distribooked.model.Author;
import it.unipi.distribooked.model.embedded.EmbeddedBookAuthor;
import it.unipi.distribooked.repository.mongo.custom.CustomAuthorRepository;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

/**
 * Implementation of the CustomAuthorRepository interface.
 * Provides the logic for updating an author's embedded books atomically.
 */
@Repository
public class CustomAuthorRepositoryImpl implements CustomAuthorRepository {

    private static final Logger logger = LoggerFactory.getLogger(CustomAuthorRepositoryImpl.class);

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public boolean updateAuthorWithBook(ObjectId authorId, EmbeddedBookAuthor book) {
        // Build the query to find the author and check if the book is not already present
        Query query = new Query(Criteria.where("_id").is(authorId)
                .and("books._id").ne(book.getId())); // Ensure the book is not already in the list

        // Define the update operation to add the book and increment the totalBooks counter
        Update update = new Update()
                .push("books", book) // Add the book to the embedded array
                .inc("totalBooks", 1); // Increment the totalBooks counter

        // Execute the atomic update
        UpdateResult result = mongoTemplate.updateFirst(query, update, Author.class);

        // Log the result and return true if the update modified a document
        if (result.getModifiedCount() > 0) {
            logger.debug("Successfully added book {} to author {}", book.getId(), authorId);
            return true;
        } else {
            logger.debug("Book {} is already present for author {}", book.getId(), authorId);
            return false;
        }
    }
}
