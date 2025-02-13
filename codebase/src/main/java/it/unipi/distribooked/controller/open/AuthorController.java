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
import it.unipi.distribooked.dto.AuthorDTO;
import it.unipi.distribooked.dto.AuthorSearchDTO;
import it.unipi.distribooked.dto.swagger.ErrorResponse;
import it.unipi.distribooked.dto.swagger.SuccessResponse;
import it.unipi.distribooked.model.embedded.EmbeddedBookAuthor;
import it.unipi.distribooked.service.AuthorService;
import it.unipi.distribooked.utils.ApiResponseUtil;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import it.unipi.distribooked.validation.ValidObjectId;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/authors")
@Tag(name = "Author-related searches", description = "Endpoints available to everyone, without authentication.")
@Validated
public class AuthorController {

    private static final Logger logger = LoggerFactory.getLogger(AuthorController.class);

    @Autowired
    private AuthorService authorService;

    /**
     * Search authors by their full name.
     *
     * This endpoint allows users to search for authors by specifying their full name.
     *
     * @param fullName Mandatory filter for the author's full name.
     * @return A list of authors matching the criteria, represented as `AuthorSearchDTO`.
     */
    @Operation(summary = "Search authors", description = "Search authors by their full name.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Authors retrieved successfully",
                    content = @Content(
                            schema = @Schema(
                                    implementation = SuccessResponse.class
                            ),
                            examples = @ExampleObject(
                                    value = """
                                {
                                  "data": {
                                    "authors": [
                                      {
                                        "id": "679cb31ab477993c5cdc08da",
                                        "fullName": "Thomas Mann",
                                        "yearOfBirth": "1875",
                                        "yearOfDeath": "1955"
                                      }
                                    ]
                                  },
                                  "message": "Authors retrieved successfully",
                                  "timestamp": "2025-02-09T17:27:31.4419328",
                                  "status": 200
                                }
                                """
                            )
                    )
            )
            ,
            @ApiResponse(responseCode = "400", description = "Invalid input parameter",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "No authors found with the given name",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchAuthors(
            @Parameter(description = "Filter by the full name of the author. This parameter is mandatory.",
                    example = "Joshua Bloch",
                    required = true,
                    in = ParameterIn.QUERY)
            @RequestParam(required = true) String fullName) {

        logger.info("Searching authors with full name: {}", fullName);

        List<AuthorSearchDTO> authors = authorService.searchAuthorsByName(fullName);
        Map<String, Object> response = new HashMap<>();
        response.put("authors", authors);

        return ApiResponseUtil.ok("Authors retrieved successfully", response);
    }

    /**
     * Retrieve detailed information about an author by their ID.
     *
     * This endpoint allows clients to retrieve all available details about a specific author,
     * including their biography and the first 20 books embedded in the author's document.
     *
     * If the author has more than 20 books, the `hasMoreBooks` field in the response will be set to `true`,
     * indicating that additional books can be retrieved using the `/authors/{authorId}/books` endpoint.
     *
     * @param authorId The unique ID of the author to retrieve.
     * @return The detailed information about the author, represented as `AuthorDTO`.
     */
    @Operation(
            summary = "Get author details",
            description = """
        Retrieve detailed information about an author by their ID, including their biography 
        and a paginated list of books. If the author has more than 20 books, the response will 
        include only the first 20 books, additional books can be fetched using the
        `/authors/{authorId}/books` endpoint.
    """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Author details retrieved successfully",
                    content = @Content(
                            schema = @Schema(
                                    implementation = SuccessResponse.class
                            ),
                            examples = @ExampleObject(
                                    value = """
                                {
                                  "data": {
                                    "author": {
                                      "id": "679cb31ab477993c5cdc08da",
                                      "fullName": "Thomas Mann",
                                      "yearOfBirth": "1875",
                                      "yearOfDeath": "1955",
                                      "avatarUrl": "https://m.media-amazon.com/images/I/91bDRRiA2fL._SY600_.jpg",
                                      "about": "Thomas Mann (1875–1955) was a German novelist and Nobel laureate, known for his psychological and symbolic narratives...",
                                      "books": [
                                        {
                                          "id": "679cb34cb477993c5cdd5e5c",
                                          "title": "Royal Highness",
                                          "subtitle": "Paperback – January 27, 2019",
                                          "authors": [
                                            {
                                              "id": null,
                                              "fullName": "Thomas Mann"
                                            },
                                            {
                                              "id": null,
                                              "fullName": "Curtis A. Cecil [Translator]"
                                            }
                                          ],
                                          "categories": [
                                            "Books",
                                            "Literature & Fiction",
                                            "Literary"
                                          ],
                                          "coverImageUrl": "https://m.media-amazon.com/images/I/51NSftca6AL._SX348_BO1,204,203,200_.jpg"
                                        },
                                        ...
                                      ]
                                    }
                                  },
                                  "message": "Author details retrieved successfully",
                                  "timestamp": "2025-02-09T17:28:56.8133314",
                                  "status": 200
                                }
                                """
                            )
                    )
            ),
            @ApiResponse(responseCode = "400", description = "Invalid author ID format",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Author not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{authorId}")
    public ResponseEntity<Map<String, Object>> getAuthorDetails(
            @Parameter(description = "The ID of the author.", required = true)
            @Valid @ValidObjectId @PathVariable String authorId) {

        logger.info("Fetching details for author with ID: {}", authorId);
        AuthorDTO authorDetails = authorService.getAuthorDetails(authorId);
        Map<String, Object> response = new HashMap<>();
        response.put("author", authorDetails);

        logger.info("Successfully retrieved details for author with ID: {}", authorId);

        return ApiResponseUtil.ok("Author details retrieved successfully", response);
    }

    /**
     * Retrieve paginated books for an author.
     *
     * This endpoint allows clients to retrieve books for a specific author in a paginated manner.
     *
     * Typically, this endpoint is used after calling `/authors/{authorId}`, when the `hasMoreBooks`
     * field in the response is `true`, indicating that the author has more than 20 books.
     *
     * Clients can use the `page` parameter to specify which set of books to retrieve, where each page
     * contains up to 20 books. For example, `page=0` retrieves books 1–20, `page=1` retrieves books 21–40, and so on.
     *
     * @param authorId The unique ID of the author to retrieve books for.
     * @param page The page number (0-based) to retrieve.
     * @return A paginated list of books for the specified author.
     */
    @Operation(
            summary = "Get paginated books",
            description = """
        Retrieve paginated books for an author. This endpoint is typically used after calling 
        `/authors/{authorId}`, to retrieve more than the first 20 books.
        Use the `page` parameter to specify which set of books to retrieve 
        (e.g., `page=0` for books 1–20, `page=1` for books 21–40, etc.) - default is 1, because the first 
        20 books are returned together with the author in `/authors/{authorId}`.
    """
    )
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
                                    "books": [
                                      {
                                        "id": "679cb360b477993c5cddf873",
                                        "title": "Buddenbrookit 1: Erään suvun rappeutumistarina",
                                        "subtitle": null,
                                        "authors": [
                                          {
                                            "id": null,
                                            "fullName": "Thomas Mann"
                                          },
                                          {
                                            "id": null,
                                            "fullName": "Siegberg Siiri 1890-1940 [Translator]"
                                          }
                                        ],
                                        "categories": null,
                                        "coverImageUrl": null
                                      },
                                      ...
                                    ],
                                    "page": 1
                                  },
                                  "message": "Books retrieved successfully",
                                  "timestamp": "2025-02-09T17:30:28.2470955",
                                  "status": 200
                                }
                                """
                            )
                    )
            ),
            @ApiResponse(responseCode = "400", description = "Invalid author ID or page number",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Author not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{authorId}/books")
    public ResponseEntity<Map<String, Object>> getAuthorBooks(
            @Parameter(description = "The ID of the author.", required = true)
            @Valid @ValidObjectId @PathVariable String authorId,

            @Parameter(description = "The page number (0-based).", required = true)
            @RequestParam(defaultValue = "1") int page) {

        logger.info("Fetching books for author with ID: {} on page: {}", authorId, page);
        List<EmbeddedBookAuthor> books = authorService.getAuthorBooks(authorId, page);

        Map<String, Object> response = new HashMap<>();
        response.put("books", books);
        response.put("page", page);

        return ApiResponseUtil.ok("Books retrieved successfully", response);
    }

}
