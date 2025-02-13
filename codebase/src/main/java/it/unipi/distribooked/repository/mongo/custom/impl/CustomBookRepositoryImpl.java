package it.unipi.distribooked.repository.mongo.custom.impl;

import it.unipi.distribooked.dto.BookUtilizationDTO;
import it.unipi.distribooked.model.Book;
import it.unipi.distribooked.repository.mongo.custom.CustomBookRepository;
import it.unipi.distribooked.repository.mongo.views.BookCatalogueView;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Point;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Collation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.redis.domain.geo.Metrics;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.stream.Collectors;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.*;

@Slf4j
@Repository
public class CustomBookRepositoryImpl implements CustomBookRepository {

    @Autowired
    private MongoTemplate mongoTemplate;

    /**
     * Finds a book by its ID and filters the embedded "branches" array to include only nearby libraries
     * within the specified maximum distance using $geoWithin.
     * Uses a projection interface for optimized data retrieval when no nearby libraries are found.
     *
     * @param bookId The ObjectId of the book to find
     * @param longitude The longitude of the search center point
     * @param latitude The latitude of the search center point
     * @param maxDistance The maximum distance in meters (defaults to 50000 if null)
     * @return Book object with either nearby libraries or empty branches list
     */
    @Override
    public Book findBookWithNearbyLibraries(ObjectId bookId, double longitude, double latitude, Integer maxDistance) {
        double searchRadius = (maxDistance != null) ? maxDistance : 50000;
        Point point = new Point(longitude, latitude);
        Circle circle = new Circle(point, new Distance(searchRadius, Metrics.METERS));

        // Try to find the book with nearby libraries using aggregation
        Aggregation aggregation = Aggregation.newAggregation(
                match(Criteria.where("_id").is(bookId)),
                Aggregation.unwind("branches", true),
                match(Criteria.where("branches.location").withinSphere(circle)),
                Aggregation.group("_id")
                        .first("title").as("title")
                        .first("subtitle").as("subtitle")
                        .first("publicationDate").as("publicationDate")
                        .first("publisher").as("publisher")
                        .first("language").as("language")
                        .first("categories").as("categories")
                        .first("isbn10").as("isbn10")
                        .first("isbn13").as("isbn13")
                        .first("coverImageUrl").as("coverImageUrl")
                        .first("authors").as("authors")
                        .push("branches").as("branches")
        );

        List<Book> results = mongoTemplate.aggregate(aggregation, "books", Book.class).getMappedResults();

        if (!results.isEmpty()) {
            return results.get(0);
        }

        // If no nearby libraries found, get only the book details using projection
        Query query = new Query(Criteria.where("_id").is(bookId));
        query.fields()
                .include("id")
                .include("title")
                .include("subtitle")
                .include("publicationDate")
                .include("publisher")
                .include("language")
                .include("categories")
                .include("isbn10")
                .include("isbn13")
                .include("coverImageUrl")
                .include("authors")
                .include("readingsCount");

        Book bookDetails = mongoTemplate.findOne(query, Book.class, "books");

        if (bookDetails == null) {
            return null;
        }

        // Set empty branches and return
        bookDetails.setBranches(List.of());
        return bookDetails;
    }

    @Override
    public Map<String, List<BookUtilizationDTO>> findBooksUtilization() {
        // Build the aggregation pipeline to compute book utilization statistics
        Aggregation aggregation = buildUtilizationAggregationPipeline();

        // Execute the aggregation query on the "books" collection
        AggregationResults<Document> results = mongoTemplate.aggregate(
                aggregation, "books", Document.class);

        // Extract the unique mapped result from the aggregation output
        Document result = results.getUniqueMappedResult();

        // Initialize response map to store categorized book utilization data
        Map<String, List<BookUtilizationDTO>> response = new HashMap<>();

        // If result exists, extract underutilized and overutilized books
        if (result != null) {
            response.put("underutilized_books",
                    mapToBookUtilizationDTOList((List<Document>) result.get("underutilized_books")));
            response.put("overutilized_books",
                    mapToBookUtilizationDTOList((List<Document>) result.get("overutilized_books")));
        }

        return response;
    }

    // Builds the aggregation pipeline to process book utilization data
    private Aggregation buildUtilizationAggregationPipeline() {
        return newAggregation(
                calculateInitialMetrics(), // Compute initial book metrics
                calculateAveragesPerBranch(), // Compute per-branch averages
                createUtilizationFacets() // Categorize books into under/over-utilized
        );
    }

    // First stage: Compute fundamental book metrics
    private ProjectionOperation calculateInitialMetrics() {
        return project()
                .and("_id").as("bookId") // Map book ID
                .and("title").as("title") // Map book title
                .and("readingsCount").as("totalReadings") // Total times book has been read
                .and(AccumulatorOperators.Sum.sumOf("branches.numberOfCopies")).as("totalCopies") // Sum of all copies across branches
                .and(ArrayOperators.Size.lengthOfArray("branches")).as("totalBranches") // Number of branches holding copies
                .and(calculateUsageRatioCondition()).as("usageRatio"); // Compute the usage ratio
    }

    // Computes usage ratio: readings count divided by total copies
    private AggregationExpression calculateUsageRatioCondition() {
        return ConditionalOperators.when(
                        ComparisonOperators.Gt.valueOf(
                                AccumulatorOperators.Sum.sumOf("branches.numberOfCopies") // If total copies > 0
                        ).greaterThanValue(0)
                )
                .then(new Document("$divide", Arrays.asList(
                        "$readingsCount", // Numerator: total readings
                        new Document("$sum", "$branches.numberOfCopies") // Denominator: sum of copies across branches
                )))
                .otherwise(0); // If no copies exist, usage ratio is 0
    }

    // Second stage: Compute per-branch average copies for each book
    private ProjectionOperation calculateAveragesPerBranch() {
        return project()
                .andInclude("bookId", "title", "totalReadings", "totalCopies", "totalBranches", "usageRatio") // Include existing fields
                .and(calculateAvgCopiesPerBranchCondition()).as("avgCopiesPerBranch"); // Compute average copies per branch
    }

    // Computes average copies per branch (total copies divided by total branches)
    private AggregationExpression calculateAvgCopiesPerBranchCondition() {
        return ConditionalOperators.when(
                        ComparisonOperators.Gt.valueOf("totalBranches").greaterThanValue(0) // If there are branches
                )
                .then(new Document("$divide", Arrays.asList("$totalCopies", "$totalBranches"))) // Compute average
                .otherwise(0); // If no branches, return 0
    }

    // Third stage: Categorizes books into underutilized and overutilized using facets
    private FacetOperation createUtilizationFacets() {
        return facet()
                .and(
                        match(Criteria.where("totalCopies").gte(10)), // Consider only books with at least 10 copies
                        sort(Sort.Direction.ASC, "usageRatio"), // Sort in ascending order (low usage first)
                        limit(10) // Select the 10 most underutilized books
                ).as("underutilized_books")
                .and(
                        match(Criteria.where("totalCopies").gte(1)), // Consider books with at least 1 copy
                        sort(Sort.Direction.DESC, "usageRatio"), // Sort in descending order (high usage first)
                        limit(10) // Select the 10 most overutilized books
                ).as("overutilized_books");
    }

    // Converts MongoDB aggregation result documents into DTO objects
    private List<BookUtilizationDTO> mapToBookUtilizationDTOList(List<Document> documents) {
        if (documents == null) {
            return Collections.emptyList(); // Return empty list if no results
        }

        return documents.stream()
                .map(doc -> {
                    // Map document fields to DTO fields
                    BookUtilizationDTO dto = new BookUtilizationDTO();
                    dto.setBookId(doc.getObjectId("bookId").toString());
                    dto.setTitle(doc.getString("title"));
                    dto.setTotalReadings(doc.getInteger("totalReadings"));
                    dto.setTotalCopies(doc.getInteger("totalCopies"));
                    dto.setTotalBranches(doc.getInteger("totalBranches"));
                    dto.setAvgCopiesPerBranch(doc.getDouble("avgCopiesPerBranch"));
                    dto.setUsageRatio(doc.getDouble("usageRatio"));
                    return dto;
                })
                .collect(Collectors.toList()); // Collect mapped DTOs into a list
    }

}