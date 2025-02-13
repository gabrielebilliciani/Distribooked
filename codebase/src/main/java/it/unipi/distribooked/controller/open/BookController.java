package it.unipi.distribooked.controller.open;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.unipi.distribooked.dto.BookCatalogueDTO;
import it.unipi.distribooked.dto.BookDTO;
import it.unipi.distribooked.dto.swagger.ErrorResponse;
import it.unipi.distribooked.dto.swagger.SuccessResponse;
import it.unipi.distribooked.service.AuthorService;
import it.unipi.distribooked.service.BookService;
import it.unipi.distribooked.utils.ApiResponseUtil;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import it.unipi.distribooked.validation.ValidObjectId;

import java.util.HashMap;
import java.util.Map;

/**
 * The BookController class provides public endpoints for browsing and searching the book catalog.
 * It also handles checking book availability and locating libraries with specific books.
 */
@RestController
@RequestMapping("/api/v1/books")
@Tag(name = "Book-related searches", description = "Endpoints available to everyone, without authentication.")
@Validated
public class BookController {

    private static final Logger logger = LoggerFactory.getLogger(BookController.class);

    @Autowired
    private BookService bookService;

    @Autowired
    private AuthorService authorService;

    /**
     * Browse the entire book catalog.
     *
     * This endpoint retrieves a paginated list of books in the catalog, including details
     * such as title, subtitle, authors, categories, and cover image URL.
     *
     * @return A list of all books in the catalog.
     */
    @Operation(summary = "Browse book catalog", description = "Retrieve a paginated list of books in the catalog.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Books retrieved successfully",
                    content = @Content(
                            schema = @Schema(
                                    implementation = SuccessResponse.class
                            ),
                            examples = @ExampleObject(
                                    value = """
                                {
                                  "data": {
                                    "content": [
                                      {
                                        "id": "679cb334b477993c5cdc8b3d",
                                        "title": "Alice's Adventures in Wonderland",
                                        "subtitle": "Hardcover – January 1, 1997",
                                        "authors": [
                                          {
                                            "id": "679cb31ab477993c5cdbf61f",
                                            "fullName": "Lewis Carroll"
                                          }
                                        ],
                                        "categories": [
                                          "Books",
                                          "Children's Books",
                                          "Classics"
                                        ],
                                        "coverImageUrl": "https://m.media-amazon.com/images/I/41SJd51ySkL._SX218_BO1,204,203,200_QL40_FMwebp_.jpg"
                                      },
                                      ...
                                    ],
                                    "page": {
                                      "size": 20,
                                      "number": 0,
                                      "totalElements": 100866,
                                      "totalPages": 5044
                                    }
                                  },
                                  "message": "Books retrieved successfully",
                                  "timestamp": "2025-02-09T17:21:30.3806508",
                                  "status": 200
                                }
                                """
                            )
                    )
            ),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "413", description = "Requested page size is too large",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping
    public ResponseEntity <Map<String, Object>> getBooks(
            @Parameter(
                    description = "Page number to retrieve. Defaults to 0 (first page).",
                    example = "0",
                    in = ParameterIn.QUERY
            )
            @RequestParam(defaultValue = "0") int page,

            @Parameter(
                    description = "Number of books per page. Must be between 1 and 100.",
                    example = "20",
                    in = ParameterIn.QUERY
            )
            @RequestParam(defaultValue = "20") @Max(100) int size) {

        logger.info("Fetching books page {} with size {}", page, size);
        Pageable pageable = PageRequest.of(page, size);

        Page<BookCatalogueDTO> books = bookService.getAllBooks(pageable);
        logger.info("Retrieved page {} of {}, {} books total",
                books.getNumber(), books.getTotalPages(), books.getTotalElements());
        return ApiResponseUtil.ok("Books retrieved successfully", books);
    }

    /**
     * Filter books by category and/or popularity.
     *
     * This endpoint allows users to filter books based on a category and/or sort them by popularity.
     * At least one filter must be provided.
     *
     * @param category        Optional filter by book category.
     * @param sortByPopularity Optional flag to sort results by popularity.
     * @param page            Page number to retrieve (default: 0).
     * @param size            Number of books per page (default: 20, max: 100).
     * @return A paginated list of books matching the filter criteria.
     */
    @Operation(summary = "Filter books", description = "Retrieve books filtered by category and/or sorted by popularity.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Books filtered successfully",
                    content = @Content(
                            schema = @Schema(
                                    implementation = SuccessResponse.class
                            ),
                            examples = @ExampleObject(
                                    value = "{\n" +
                                            "  \"data\": {\n" +
                                            "    \"content\": [\n" +
                                            "      {\n" +
                                            "        \"id\": \"679cb339b477993c5cdcbf94\",\n" +
                                            "        \"title\": \"The Time Machine\",\n" +
                                            "        \"subtitle\": \"Paperback – March 21, 2014\",\n" +
                                            "        \"authors\": [\n" +
                                            "          {\n" +
                                            "            \"id\": \"679cb31ab477993c5cdbf638\",\n" +
                                            "            \"fullName\": \"H. G. (Herbert George) Wells\"\n" +
                                            "          }\n" +
                                            "        ],\n" +
                                            "        \"categories\": [\n" +
                                            "          \"Books\",\n" +
                                            "          \"Science Fiction & Fantasy\",\n" +
                                            "          \"Science Fiction\"\n" +
                                            "        ],\n" +
                                            "        \"coverImageUrl\": null\n" +
                                            "      },\n" +
                                            "      {\n" +
                                            "        \"id\": \"679cb34ab477993c5cdd4f0f\",\n" +
                                            "        \"title\": \"Triplanetary\",\n" +
                                            "        \"subtitle\": \"Mass Market Paperback – January 1, 1973\",\n" +
                                            "        \"authors\": [\n" +
                                            "          {\n" +
                                            "            \"id\": \"679cb31ab477993c5cdc1bc7\",\n" +
                                            "            \"fullName\": \"E. E. (Edward Elmer) Smith\"\n" +
                                            "          }\n" +
                                            "        ],\n" +
                                            "        \"categories\": [\n" +
                                            "          \"Books\",\n" +
                                            "          \"Science Fiction & Fantasy\",\n" +
                                            "          \"Science Fiction\"\n" +
                                            "        ],\n" +
                                            "        \"coverImageUrl\": null\n" +
                                            "      },\n" +
                                            "      ...\n" +
                                            "    ],\n" +
                                            "    \"page\": {\n" +
                                            "      \"size\": 20,\n" +
                                            "      \"number\": 0,\n" +
                                            "      \"totalElements\": 659,\n" +
                                            "      \"totalPages\": 33\n" +
                                            "    }\n" +
                                            "  },\n" +
                                            "  \"message\": \"Books filtered successfully\",\n" +
                                            "  \"timestamp\": \"2025-02-09T17:06:34.1979988\",\n" +
                                            "  \"status\": 200\n" +
                                            "}"
                            )
                    )
            ),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/filter")
    public ResponseEntity<Map<String, Object>> filterBooks(
            @Parameter(description = "Filter by book category", example = "Science Fiction", in = ParameterIn.QUERY)
            @RequestParam(required = false) String category,

            @Parameter(description = "Sort by popularity", example = "true", in = ParameterIn.QUERY)
            @RequestParam(required = false) Boolean sortByPopularity,

            @Parameter(description = "Page number to retrieve", example = "0", in = ParameterIn.QUERY)
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Number of books per page (max 100)", example = "20", in = ParameterIn.QUERY)
            @RequestParam(defaultValue = "20") @Max(100) int size
    ) {
        logger.info("Filtering books - category: {}, sortByPopularity: {}, page: {}, size: {}", category, sortByPopularity, page, size);

        // Validation: require at least one filter
        if (category == null && (sortByPopularity == null || !sortByPopularity)) {
            logger.warn("Invalid filter request: At least one filter (category or sortByPopularity) must be specified");
            throw new IllegalArgumentException("At least one filter (category or sortByPopularity) must be specified");
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<BookCatalogueDTO> filteredBooks = bookService.filterBooks(category, sortByPopularity, pageable);

        logger.info("Retrieved {} books matching the filter criteria", filteredBooks.getTotalElements());
        return ApiResponseUtil.ok("Books filtered successfully", filteredBooks);
    }


    /**
     * Search books by title and optionally by author.
     *
     * This endpoint allows users to search for books by specifying a title. If only the title is provided,
     * the search requires an exact match with the full title. However, if an author is also specified,
     * the system performs a more flexible search that matches partial titles (case-insensitive).
     *
     * POSSIBLE IMPROVEMENT: elastic search
     *
     * @param title  Mandatory filter for the book title.
     * @param author Optional filter for the author's name.
     * @return A list of books matching the criteria, represented as `BookCatalogueDTO`.
     * @throws IllegalArgumentException if the title is not provided.
     */
    @Operation(summary = "Search books", description = "Search books by title (exact match) and optionally by author (allows partial matches on title if author's name matches).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Books retrieved successfully",
                    content = @Content(
                            schema = @Schema(
                                    implementation = SuccessResponse.class
                            ),
                            examples = @ExampleObject(
                                    value = """
                                {
                                  "data": {
                                    "totalItems": 1,
                                    "books": [
                                      {
                                        "id": "679cb33db477993c5cdcdf9c",
                                        "title": "Der Tod in Venedig",
                                        "subtitle": null,
                                        "authors": [
                                          {
                                            "id": "679cb31ab477993c5cdc08da",
                                            "fullName": "Thomas Mann"
                                          }
                                        ],
                                        "categories": null,
                                        "coverImageUrl": null
                                      }
                                    ],
                                    "totalPages": 1,
                                    "currentPage": 0
                                  },
                                  "message": "Books retrieved successfully",
                                  "timestamp": "2025-02-09T17:22:58.8306423",
                                  "status": 200
                                }
                                """
                            )
                    )
            ),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "No books found matching the specified criteria",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchBooks(
            @Parameter(description = "Filter by book title. This parameter is mandatory. Requires an exact match if no author is specified.",
                    example = "Effective Java",
                    required = true,
                    in = ParameterIn.QUERY)
            @RequestParam(required = true) String title,

            @Parameter(description = "Filter by author name. This parameter is optional. Enables a more flexible search for partial titles.",
                    example = "Joshua Bloch",
                    in = ParameterIn.QUERY)
            @RequestParam(required = false) String author,

            @RequestParam(defaultValue = "0") int page) {

        logger.info("Searching books with title: {}, author: {}, page: {}", title, author, page);

        Page<BookCatalogueDTO> bookPage = bookService.searchBooks(title, author, page);

        Map<String, Object> response = new HashMap<>();
        response.put("books", bookPage.getContent());
        response.put("currentPage", bookPage.getNumber());
        response.put("totalPages", bookPage.getTotalPages());
        response.put("totalItems", bookPage.getTotalElements());
        logger.info("Found {} books matching the search criteria", bookPage.getTotalElements());
        return ApiResponseUtil.ok("Books retrieved successfully", response);
    }

    /**
     * Get details of a specific book and optionally nearby libraries with the book.
     *
     * @param id The unique ID of the book.
     * @param latitude Optional latitude of the user for proximity search.
     * @param longitude Optional longitude of the user for proximity search.
     * @param radius Optional maximum distance (in meters) for proximity search.
     * @return The details of the requested book, represented as `BookDTO`.
     */
    @Operation(summary = "Get book details with optional library proximity search",
            description = "Retrieve details of a specific book by its ID. If latitude and longitude are provided, the response will include only libraries near that location. " +
                    "An optional radius (in meters) can be specified for filtering libraries within that range; otherwise, a default of 50km is applied.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Book details retrieved successfully",
                    content = @Content(
                            schema = @Schema(
                                    implementation = SuccessResponse.class
                            ),
                            examples = @ExampleObject(
                                    value = "{\n" +
                                            "  \"data\": {\n" +
                                            "    \"id\": \"679cb34ab477993c5cdd4f0f\",\n" +
                                            "    \"title\": \"Triplanetary\",\n" +
                                            "    \"subtitle\": \"Mass Market Paperback – January 1, 1973\",\n" +
                                            "    \"publicationDate\": \"2010-06-06\",\n" +
                                            "    \"language\": \"English\",\n" +
                                            "    \"categories\": [\n" +
                                            "      \"Books\",\n" +
                                            "      \"Science Fiction & Fantasy\",\n" +
                                            "      \"Science Fiction\"\n" +
                                            "    ],\n" +
                                            "    \"isbn10\": \"0515028908\",\n" +
                                            "    \"isbn13\": \"978-0515028904\",\n" +
                                            "    \"publisher\": \"Pyramid (January 1, 1973)\",\n" +
                                            "    \"coverImageUrl\": null,\n" +
                                            "    \"authors\": [\n" +
                                            "      {\n" +
                                            "        \"id\": \"679cb31ab477993c5cdc1bc7\",\n" +
                                            "        \"fullName\": \"E. E. (Edward Elmer) Smith\"\n" +
                                            "      }\n" +
                                            "    ],\n" +
                                            "    \"branches\": [\n" +
                                            "      {\n" +
                                            "        \"id\": \"679cb364d125ba32463b9759\",\n" +
                                            "        \"libraryName\": \"Biblioteca di Scienze naturali e ambientali dell'Università di Pisa\",\n" +
                                            "        \"coordinates\": [\n" +
                                            "          10.3953125,\n" +
                                            "          43.7185718\n" +
                                            "        ],\n" +
                                            "        \"address\": \"Via Derna 1, Pisa, Pisa 56126, Italia\",\n" +
                                            "        \"numberOfCopies\": 2\n" +
                                            "      },\n" +
                                            "      {\n" +
                                            "        \"id\": \"679cb364d125ba32463b9749\",\n" +
                                            "        \"libraryName\": \"Biblioteca di Ingegneria dell'Università di Pisa\",\n" +
                                            "        \"coordinates\": [\n" +
                                            "          10.3896690589,\n" +
                                            "          43.7207286212\n" +
                                            "        ],\n" +
                                            "        \"address\": \"Largo Lucio Lazzarino 1, Pisa, Pisa 56122, Italia\",\n" +
                                            "        \"numberOfCopies\": 5\n" +
                                            "      },\n" +
                                            "      {\n" +
                                            "        \"id\": \"679cb364d125ba32463b974e\",\n" +
                                            "        \"libraryName\": \"Biblioteca di Antichistica, Linguistica, Germanistica, Slavistica dell'Università di Pisa\",\n" +
                                            "        \"coordinates\": [\n" +
                                            "          10.3974391,\n" +
                                            "          43.7195217\n" +
                                            "        ],\n" +
                                            "        \"address\": \"Via Santa Maria 44, Pisa, Pisa 56126, Italia\",\n" +
                                            "        \"numberOfCopies\": 3\n" +
                                            "      },\n" +
                                            "      {\n" +
                                            "        \"id\": \"679cb364d125ba32463b974f\",\n" +
                                            "        \"libraryName\": \"Biblioteca di italianistica e romanistica dell'Università di Pisa\",\n" +
                                            "        \"coordinates\": [\n" +
                                            "          10.3981386135,\n" +
                                            "          43.7182307403\n" +
                                            "        ],\n" +
                                            "        \"address\": \"Piazza Torricelli 2, Pisa, Pisa 56126, Italia\",\n" +
                                            "        \"numberOfCopies\": 5\n" +
                                            "      }\n" +
                                            "    ],\n" +
                                            "    \"readingsCount\": 5\n" +
                                            "  },\n" +
                                            "  \"message\": \"Book details retrieved successfully\",\n" +
                                            "  \"timestamp\": \"2025-02-09T17:06:55.3797938\",\n" +
                                            "  \"status\": 200\n" +
                                            "}"
                            )
                    )
            ),
            @ApiResponse(responseCode = "400", description = "Invalid input for coordinates or radius",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Book not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getBookByIdWithLibraries(
            @Valid @ValidObjectId @PathVariable String id,
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude,
            @RequestParam(required = false) Integer radius) {

        logger.info("Fetching details for book with ID: {}", id);

        // Validation: Latitude and longitude must be provided together
        if ((latitude == null && longitude != null) || (latitude != null && longitude == null)) {
            throw new IllegalArgumentException("Both latitude and longitude must be provided together.");
        }

        // Validation: Radius is allowed only if latitude & longitude are provided
        if (radius != null && (latitude == null || longitude == null)) {
            throw new IllegalArgumentException("Radius can only be specified when both latitude and longitude are provided.");
        }

        // Delegate logic to the service
        BookDTO book = bookService.getBookByIdWithLibraries(id, latitude, longitude, radius);

        logger.info("Book details and library availability retrieved for ID: {}", id);
        return ApiResponseUtil.ok("Book details retrieved successfully", book);
    }


    /**
     * Check the real availability of a specific book in a specific library.
     *
     * @param bookId The ID of the book.
     * @param libraryId The ID of the library.
     * @return The number of available copies.
     */
    @Operation(summary = "Check book availability in a specific library",
            description = "Retrieve the real availability of a specific book in a specific library.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Availability retrieved successfully",
                    content = @Content(
                            schema = @Schema(
                                    implementation = SuccessResponse.class
                            ),
                            examples = @ExampleObject(
                                    value = """
                                {
                                  "data": 5,
                                  "message": "Availability retrieved successfully",
                                  "timestamp": "2025-02-09T17:24:31.0175692",
                                  "status": 200
                                }
                                """
                            )
                    )
            ),
            @ApiResponse(responseCode = "404", description = "Book or library not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{bookId}/availability/{libraryId}")
    public
    ResponseEntity<Map<String, Object>> getBookAvailability(
            @Valid @ValidObjectId @PathVariable String bookId,
            @Valid @ValidObjectId @PathVariable String libraryId) {
        logger.info("Checking availability of book {} in library {}", bookId, libraryId);
        Integer availableCopies = bookService.getBookAvailability(bookId, libraryId);
        logger.info("Book {} has {} available copies in library {}", bookId, availableCopies, libraryId);
        return ApiResponseUtil.ok("Availability retrieved successfully", availableCopies);
    }

}
