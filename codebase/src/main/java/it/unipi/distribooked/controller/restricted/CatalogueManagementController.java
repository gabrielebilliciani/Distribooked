package it.unipi.distribooked.controller.restricted;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.unipi.distribooked.dto.BookDTO;
import it.unipi.distribooked.dto.LibraryDTO;
import it.unipi.distribooked.dto.swagger.CreatedResponse;
import it.unipi.distribooked.dto.swagger.ErrorResponse;
import it.unipi.distribooked.dto.swagger.SuccessResponse;
import it.unipi.distribooked.model.Library;
import it.unipi.distribooked.service.CatalogueManagementService;
import it.unipi.distribooked.service.LibraryService;
import it.unipi.distribooked.utils.ApiResponseUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import it.unipi.distribooked.validation.ValidObjectId;

import java.util.Map;

/**
 * The CatalogueManagementController class manages the administration of the book catalog.
 * It provides endpoints for adding, updating, and deleting books from the catalog.
 */
@RestController
@RequestMapping("/api/v1/admin/catalogue")
@Tag(name = "Catalogue Management", description = "Operations related to catalogue management, for admins only.")
@Validated
public class CatalogueManagementController {

    private static final Logger logger = LoggerFactory.getLogger(CatalogueManagementController.class);

    @Autowired
    private CatalogueManagementService catalogueManagementService;

    @Autowired
    private LibraryService libraryService;

    /**
     * Adds a new book to the catalog.
     *
     * @param bookRequest The details of the book to be added.
     * @return The details of the added book.
     */
    @PostMapping("/books")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "(ADMIN) Add a book to the catalog",
            description = """
        Allows an admin to add a new book to the catalog.
        This endpoint is reserved for administrators.
        The new book must not include the `branches` field, as libraries stocking the book should be added separately.
        Additionally, all authors referenced in the request must already exist in the `authors` collection.
        """)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Book added successfully",
                    content = @Content(
                            schema = @Schema(
                                    implementation = CreatedResponse.class
                            ),
                            examples = @ExampleObject(
                                    value = """
                                {
                                  "path": "/api/v1/admin/catalogue/books/67a8de6e0ffe182e3ab5ea2c",
                                  "data": {
                                    "id": "67a8de6e0ffe182e3ab5ea2c",
                                    "title": "The Revolutionary Ideas of Thomas Jefferson (II print)",
                                    "subtitle": "A Perspective on Liberty",
                                    "publicationDate": "2023-01-01",
                                    "language": "English",
                                    "categories": [
                                      "History",
                                      "Biography",
                                      "Politics"
                                    ],
                                    "isbn10": "1234567890",
                                    "isbn13": "9781234567890",
                                    "publisher": "Freedom Press",
                                    "coverImageUrl": "https://example.com/cover.jpg",
                                    "authors": [
                                      {
                                        "id": "679cb31ab477993c5cdbf61a",
                                        "fullName": "Thomas Jefferson"
                                      }
                                    ],
                                    "readingsCount": 0
                                  },
                                  "message": "Book added successfully",
                                  "timestamp": "2025-02-09T17:57:18.6232808",
                                  "status": 201
                                }
                                """
                            )
                    )
            ),
            @ApiResponse(responseCode = "400", description = "Invalid book details",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Please login to access this resource.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Insufficient privileges to access this resource.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Book already exists",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Map<String, Object>> addBook(
            @Valid @RequestBody @Schema(example = """
        {
            "title": "The Revolutionary Ideas of Thomas Jefferson",
            "subtitle": "A Perspective on Liberty",
            "publicationDate": "2025-01-01",
            "language": "English",
            "categories": ["History", "Biography", "Politics"],
            "isbn10": "1234567890",
            "isbn13": "9781234567890",
            "publisher": "Freedom Press",
            "coverImageUrl": "https://example.com/cover.jpg",
            "authors": [
                {
                    "id": "6793b53c67e6767edfe2a3c9",
                    "fullName": "Thomas Jefferson"
                }
            ]
        }
        """) BookDTO bookRequest) {
        logger.info("Adding a new book to the catalog: {}", bookRequest);
        BookDTO response = catalogueManagementService.addBook(bookRequest);
        logger.info("Book added successfully: {}", response);

        return ApiResponseUtil.created(
                "Book added successfully",
                response,
                "/api/v1/admin/catalogue/books/" + response.getId()
        );
    }

    /**
     * Decrement the number of available copies of a book in a specific library.
     *
     * @param bookId     The ID of the book.
     * @param libraryId  The ID of the library.
     * @return A response indicating success or failure.
     */
    @PatchMapping("/books/{bookId}/libraries/{libraryId}/decrement")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "(ADMIN) Decrement available copies of a book in a library",
            description = "Allows an admin to decrement the number of available copies of a book in a specific library.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Copies decremented successfully",
                    content = @Content(schema = @Schema(implementation = SuccessResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request or no available copies.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Please login to access this resource.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Insufficient privileges to access this resource.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Book or library not found.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Map<String, Object>> decrementAvailableCopies(
            @Valid @ValidObjectId @PathVariable String bookId,
            @Valid @ValidObjectId  @PathVariable String libraryId) {
        logger.info("Decrementing available copies of book {} in library {}", bookId, libraryId);
        catalogueManagementService.decrementCopiesInLibrary(bookId, libraryId);
        logger.info("Copies decremented successfully for book {} in library {}", bookId, libraryId);

        return ApiResponseUtil.ok(
                "Copies decremented successfully",
                null
        );
    }

    /**
     * Remove a book entirely from a specific library.
     *
     * @param bookId     The ID of the book.
     * @param libraryId  The ID of the library.
     * @return A response indicating success or failure.
     */
    @DeleteMapping("/books/{bookId}/libraries/{libraryId}")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "(ADMIN) Remove a book from a library",
            description = "Allows an admin to completely remove a book from a specific library.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Book removed from library successfully",
                    content = @Content(schema = @Schema(implementation = SuccessResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Please login to access this resource.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Insufficient privileges to access this resource.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Book or library not found.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Map<String, Object>> removeBookFromLibrary(
            @Valid @ValidObjectId @PathVariable String bookId,
            @Valid @ValidObjectId @PathVariable String libraryId) {
        logger.info("Attempting to remove book {} from library {}", bookId, libraryId);
        catalogueManagementService.removeBookFromLibrary(bookId, libraryId);
        logger.info("Book removed successfully from library {}", libraryId);

        return ApiResponseUtil.ok(
                "Book removed from library successfully",
                null
        );
    }

    @PatchMapping("/books/{bookId}/libraries/{libraryId}/increment")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "(ADMIN) Increment available copies of a book in a library",
            description = "Allows an admin to increment the number of available copies of a book in a specific library.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Copies incremented successfully",
                    content = @Content(schema = @Schema(implementation = SuccessResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Please login to access this resource.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Insufficient privileges to access this resource.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Book or library not found.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Map<String, Object>> incrementAvailableCopies(
            @Valid @ValidObjectId @PathVariable String bookId,
            @Valid @ValidObjectId @PathVariable String libraryId) {
        logger.info("Attempting increment of available copies of book {} in library {}", bookId, libraryId);
        catalogueManagementService.incrementCopiesInLibrary(bookId, libraryId);
        logger.info("Copies incremented successfully for book {} in library {}", bookId, libraryId);

        return ApiResponseUtil.ok(
                "Copies incremented successfully",
                null
        );
    }

    @PostMapping("/books/{bookId}/libraries/{libraryId}")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "(ADMIN) Add a library to a book's availability",
            description = "Allows an admin to add a library to the availability of a book.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Library added to book availability successfully",
                    content = @Content(schema = @Schema(implementation = SuccessResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Please login to access this resource.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Insufficient privileges to access this resource.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Book or library not found.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Map<String, Object>> addLibraryToBookAvailability(
            @Valid @ValidObjectId @PathVariable String bookId,
            @Valid @ValidObjectId @PathVariable String libraryId,
            @RequestParam(required = false) Integer initialValue) {
        logger.info("Adding library {} to availability of book {} with initial value {}", libraryId, bookId, initialValue);
        catalogueManagementService.addLibraryToBookAvailability(bookId, libraryId, initialValue != null ? initialValue : 0);
        logger.info("Library {} added to availability of book {} with initial value {}", libraryId, bookId, initialValue);

        return ApiResponseUtil.ok(
                "Library added to book availability successfully",
                null
        );
    }


    @PostMapping("/libraries")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "(ADMIN) Add a new library",
            description = "Allows an admin to add a new library to the collection. A library must have a unique combination of name and postal code.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Library added successfully.",
                    content = @Content(
                            schema = @Schema(
                                    implementation = CreatedResponse.class
                            ),
                            examples = @ExampleObject(
                                    value = """
                                {
                                  "path": "/api/v1/admin/catalogue/libraries/64b87f1a2d3b9c1234567890",
                                  "data": {
                                    "id": "64b87f1a2d3b9c1234567890",
                                    "name": "Biblioteca degli Universitari Toscani",
                                    "address": {
                                      "street": "Via Roma, 10",
                                      "city": "Firenze",
                                      "province": "FI",
                                      "postalCode": "50123",
                                      "country": "Italy"
                                    },
                                    "postalCode": "12345",
                                    "province": "Province A",
                                    "region": "Region B",
                                    "provinceIstatCode": "654321",
                                    "latitude": 43.7942,
                                    "longitude": 11.2468",
                                    "phone": "123-456-7890",
                                    "email": "info@library.com",
                                    "url": "http://library.com"
                                  },
                                  "message": "Library added successfully",
                                  "timestamp": "2025-02-09T18:12:33.8579964",
                                  "status": 201
                                }
                                """
                            )
                    )
            ),
            @ApiResponse(responseCode = "400", description = "Invalid library details.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Please login to access this resource.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Insufficient privileges to access this resource.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "A library with the same name and postal code already exists.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Map<String, Object>> addLibrary(@Valid @RequestBody LibraryDTO libraryRequest) {
        logger.info("Adding a new library: {}", libraryRequest);
        libraryService.addLibrary(libraryRequest);
        logger.info("Library added successfully: {}", libraryRequest);

        return ApiResponseUtil.created(
                "Library added successfully",
                libraryRequest,
                "/api/v1/admin/catalogue/libraries/" + libraryRequest.getId()
        );
    }

}
