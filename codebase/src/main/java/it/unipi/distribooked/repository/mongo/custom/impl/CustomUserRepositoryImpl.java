package it.unipi.distribooked.repository.mongo.custom.impl;


import com.mongodb.client.result.UpdateResult;
import it.unipi.distribooked.dto.BooksByAgeGroupDTO;
import it.unipi.distribooked.exceptions.BookNotSavedException;
import it.unipi.distribooked.model.User;
import it.unipi.distribooked.model.embedded.EmbeddedBookRead;
import it.unipi.distribooked.model.embedded.EmbeddedBookSaved;
import it.unipi.distribooked.repository.mongo.custom.CustomUserRepository;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;
import it.unipi.distribooked.exceptions.BookSaveException;


import java.util.Arrays;

import java.util.List;
import java.util.Map;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.*;
import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;


import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;


import java.util.HashMap;


/**
 * Implementation of the custom UserRepository for custom MongoDB operations.
 */
@Repository
public class CustomUserRepositoryImpl implements CustomUserRepository {

    Logger logger = LoggerFactory.getLogger(CustomUserRepositoryImpl.class);

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public boolean unsaveBook(String userId, String bookId) {
        Query query = new Query(Criteria.where("_id").is(new ObjectId(userId)));
        Update update = new Update().pull("savedBooks",
                Query.query(Criteria.where("id").is(new ObjectId(bookId)))
        );

        UpdateResult result = mongoTemplate.updateFirst(query, update, User.class);

        if (result.getModifiedCount() == 0) {
            throw new BookNotSavedException(userId, bookId);
        }

        return true;
    }

    @Override
    public long saveBook(String userId, EmbeddedBookSaved bookToSave) {
        Query query = new Query(
                Criteria.where("_id").is(new ObjectId(userId))
                        .and("savedBooks").not().size(50)
                        .and("savedBooks.id").ne(bookToSave.getId())
        );

        logger.info("Saving book for user: {}", userId);

        Update update = new Update().push("savedBooks", bookToSave);

        UpdateResult result = mongoTemplate.updateFirst(query, update, User.class);

        if (result.getModifiedCount() == 0) {
            // Check why the save failed
            User user = mongoTemplate.findOne(
                    query(where("_id").is(new ObjectId(userId))),
                    User.class
            );

            if (user.getSavedBooks().size() >= 50) {
                throw BookSaveException.maxLimitReached();
            }

            throw BookSaveException.alreadySaved();
        }

        return result.getModifiedCount();
    }

    @Override
    public boolean addReadBook(ObjectId userId, EmbeddedBookRead bookRead) {
        Query query = new Query(Criteria.where("_id").is(userId));
        Update update = new Update().push("readings", bookRead);

        UpdateResult result = mongoTemplate.updateFirst(query, update, User.class);

        // return true if the book was added successfully
        return result.getModifiedCount() > 0;
    }

    @Override
    public List<BooksByAgeGroupDTO> findMostReadBooksByAgeGroup(String startDate, String endDate) {

        logger.info("Starting findMostReadBooksByAgeGroup with startDate={} and endDate={}", startDate, endDate);

        // Build the aggregation pipeline for retrieving most-read books per age group
        Aggregation aggregation = buildAggregationPipeline(startDate, endDate);

        // Execute the aggregation query and return the results
        return executeAggregation(aggregation);
    }

    private Aggregation buildAggregationPipeline(String startDate, String endDate) {
        return newAggregation(

                // Initial filter: retain only users who have readings within the specified date range
                // This step optimizes performance by excluding irrelevant documents early on
                Aggregation.match(
                        Criteria.where("readings").elemMatch(
                                Criteria.where("returnDate").gte(startDate).lte(endDate)
                        )
                ),

                // Compute the user's age group before unwinding the readings array
                // This step avoids recalculating the age group for each reading record
                calculateAgeGroupField(),

                // Retain only the necessary fields: computed age group and readings array
                Aggregation.project()
                        .andInclude("age_group")
                        .andInclude("readings"),

                // Unwind the readings array to process individual reading records
                Aggregation.unwind("readings"),

                // Filter out readings that do not fall within the specified date range
                Aggregation.match(Criteria.where("readings.returnDate").gte(startDate).lte(endDate)),

                // Group by age group and book ID, counting the number of times each book was read
                group("age_group", "readings.id", "readings.title")
                        .count().as("readCount"),

                // Sort the results by the number of times a book was read, in descending order
                Aggregation.sort(Sort.Direction.DESC, "readCount"),

                // Group again by age group, aggregating the most-read books
                groupByAgeWithTopBooks(),

                // Format the final output structure
                projectFinalResults()
        );
    }

    /**
     * Computes the age group of each user based on their date of birth.
     * Uses MongoDB's `$dateDiff` to calculate the user's age in years and assigns them to predefined age groups.
     */
    private AggregationOperation calculateAgeGroupField() {
        return ctx -> new Document("$addFields",
                new Document("age_group",
                        new Document("$switch",
                                new Document("branches", Arrays.asList(
                                        createAgeGroupCase(18, 30, "18-29"),
                                        createAgeGroupCase(30, 50, "30-49"),
                                        createSeniorAgeGroupCase()
                                )).append("default", "Unknown") // Default group if no condition matches
                        )
                )
        );
    }

    /**
     * Creates a condition for categorizing users within a specified age range.
     * If the user's age falls between `minAge` and `maxAge`, they are assigned to `groupName`.
     */
    private Document createAgeGroupCase(int minAge, int maxAge, String groupName) {
        return new Document("case",
                new Document("$and", Arrays.asList(
                        new Document("$gte", Arrays.asList(
                                new Document("$dateDiff",
                                        new Document("startDate", new Document("$toDate", "$dateOfBirth"))
                                                .append("endDate", "$$NOW")
                                                .append("unit", "year")),
                                minAge
                        )),
                        new Document("$lt", Arrays.asList(
                                new Document("$dateDiff",
                                        new Document("startDate", new Document("$toDate", "$dateOfBirth"))
                                                .append("endDate", "$$NOW")
                                                .append("unit", "year")),
                                maxAge
                        ))
                ))).append("then", groupName);
    }

    /**
     * Defines a condition for users aged 50 or older.
     */
    private Document createSeniorAgeGroupCase() {
        return new Document("case",
                new Document("$gte", Arrays.asList(
                        new Document("$dateDiff",
                                new Document("startDate", new Document("$toDate", "$dateOfBirth"))
                                        .append("endDate", "$$NOW")
                                        .append("unit", "year")),
                        50
                ))).append("then", "50+");
    }

    /**
     * Groups results by age group and aggregates the most-read books for each group.
     * Stores book details and the total number of readings per age group.
     */
    private AggregationOperation groupByAgeWithTopBooks() {
        return ctx -> new Document("$group",
                new Document("_id", "$_id.age_group")
                        .append("mostReadBooks",
                                new Document("$push",
                                        new Document("bookId", "$_id.id")
                                                .append("bookTitle", "$_id.title")
                                                .append("readCount", "$readCount")))
                        .append("totalReadings", new Document("$sum", "$readCount")));
    }

    /**
     * Formats the final aggregation output, renaming fields and limiting most-read books to the top 10.
     */
    private AggregationOperation projectFinalResults() {
        return ctx -> new Document("$project",
                new Document("_id", 0)
                        .append("ageGroup", "$_id")
                        .append("totalReadings", 1)
                        .append("mostReadBooks",
                                new Document("$slice", Arrays.asList("$mostReadBooks", 10))));
    }

    /**
     * Executes the aggregation query on the "users" collection.
     * Maps the results to `BooksByAgeGroupDTO` objects and logs the number of results found.
     */
    private List<BooksByAgeGroupDTO> executeAggregation(Aggregation aggregation) {
        AggregationResults<BooksByAgeGroupDTO> results =
                mongoTemplate.aggregate(aggregation, "users", BooksByAgeGroupDTO.class);
        List<BooksByAgeGroupDTO> mappedResults = results.getMappedResults();
        logger.info("Aggregation returned {} results", mappedResults.size());
        return mappedResults;
    }

    @Override
    public Map<String, Map<String, Object>> findAverageAgeOfReadersByCity() {
        // Compute the date 365 days ago from today
        String oneYearAgo = java.time.LocalDate.now().minusDays(365).toString();

        Aggregation aggregation = newAggregation(
                // Stage 1: Filter users who have at least one reading in the last 365 days
                match(Criteria.where("readings.returnDate").gte(oneYearAgo)),

                // Stage 2: Calculate user's age
                addFields().addField("age").withValue(
                        new Document("$dateDiff", new Document("startDate", new Document("$toDate", "$dateOfBirth"))
                                .append("endDate", "$$NOW")
                                .append("unit", "year")
                        )
                ).build(),

                // Stage 3: Group by city, calculate the average age, and count total users
                group("$address.city")
                        .avg("$age").as("averageAge")
                        .count().as("totalUsers"),

                // Stage 4: Format the final output
                project("averageAge", "totalUsers")
                        .and("_id").as("city")
        );

        AggregationResults<Document> results = mongoTemplate.aggregate(aggregation, "users", Document.class);
        List<Document> mappedResults = results.getMappedResults();

        // Convert results into a Map
        Map<String, Map<String, Object>> ageStatsByCity = new HashMap<>();
        for (Document doc : mappedResults) {
            Map<String, Object> cityStats = new HashMap<>();
            cityStats.put("average_age", doc.getDouble("averageAge"));
            cityStats.put("total_users", doc.getInteger("totalUsers"));
            ageStatsByCity.put(doc.getString("city"), cityStats);
        }

        return ageStatsByCity;
    }
}
