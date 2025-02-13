package it.unipi.distribooked.controller.secured;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.unipi.distribooked.dto.EmbeddedBookReadDTO;
import it.unipi.distribooked.dto.EmbeddedBookSavedDTO;
import it.unipi.distribooked.dto.UserActiveBookDTO;
import it.unipi.distribooked.dto.UserDetailsDTO;
import it.unipi.distribooked.dto.swagger.ErrorResponse;
import it.unipi.distribooked.dto.swagger.SuccessResponse;
import it.unipi.distribooked.service.UserService;
import it.unipi.distribooked.utils.ApiResponseUtil;
import it.unipi.distribooked.utils.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * The UserController class manages user-specific secured endpoints.
 * It provides functionalities to retrieve user-specific book data, such as reserved, loaned, saved, and read books.
 */
@RestController
@Tag(name = "User", description = "Operations related to users, for registered users.")
@RequestMapping("/api/v1/users")
@Validated
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserService userService;

    @Autowired
    private SecurityUtils securityUtils;

    /**
     * Retrieves the list of books reserved and loaned by the current user.
     *
     * @return A list of reserved and loaned books.
     */
    @GetMapping("/reserved-loaned")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Get reserved and loaned books", description = "Returns the list of books currently reserved or loaned by the user.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved reserved and loaned books.",
                    content = @Content(
                            schema = @Schema(
                                    implementation = SuccessResponse.class
                            ),
                            examples = @ExampleObject(
                                    value = "{\n" +
                                            "  \"data\": {\n" +
                                            "    \"reservedAndLoanedBooks\": [\n" +
                                            "      {\n" +
                                            "        \"bookId\": \"679cb34ab477993c5cdd4f0f\",\n" +
                                            "        \"libraryId\": \"679cb364d125ba32463b9759\",\n" +
                                            "        \"libraryName\": \"Biblioteca di Scienze naturali e ambientali dell'Università di Pisa\",\n" +
                                            "        \"title\": \"Triplanetary\",\n" +
                                            "        \"status\": \"RESERVED\",\n" +
                                            "        \"deadlineDate\": 1739376697092\n" +
                                            "      }\n" +
                                            "    ]\n" +
                                            "  },\n" +
                                            "  \"message\": \"Reserved and loaned books retrieved successfully.\",\n" +
                                            "  \"timestamp\": \"2025-02-09T17:12:57.9786811\",\n" +
                                            "  \"status\": 200\n" +
                                            "}"
                            )
                    )
            ),
            @ApiResponse(responseCode = "401", description = "Please login to access this resource.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Insufficient privileges to access this resource.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Map<String, Object>> getReservedAndLoanedBooks() {
        String userId = securityUtils.getCurrentUserId();
        logger.info("Fetching reserved and loaned books for user: {}", userId);

        List<UserActiveBookDTO> reservedAndLoanedBooks = userService.getReservedAndLoanedBooks(userId);

        logger.info("Successfully retrieved reserved and loaned books for user: {}", userId);
        return ApiResponseUtil.ok(
                "Reserved and loaned books retrieved successfully.",
                Map.of("reservedAndLoanedBooks", reservedAndLoanedBooks)
        );
    }

    /**
     * Retrieves the list of books saved by the current user.
     *
     * @return A list of saved books.
     */
    @GetMapping("/saved")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Get saved books", description = "Returns the list of books saved by the current user.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved saved books.",
                    content = @Content(
                            schema = @Schema(
                                    implementation = SuccessResponse.class
                            ),
                            examples = @ExampleObject(
                                    value = """
                                {
                                  "data": {
                                    "savedBooks": [
                                      {
                                        "id": "679cb34ab477993c5cdd4f0f",
                                        "title": "Triplanetary",
                                        "authors": [
                                          {
                                            "id": "679cb31ab477993c5cdc1bc7",
                                            "fullName": "E. E. (Edward Elmer) Smith"
                                          }
                                        ]
                                      }
                                    ]
                                  },
                                  "message": "Saved books retrieved successfully.",
                                  "timestamp": "2025-02-09T17:15:59.9241197",
                                  "status": 200
                                }
                                """
                            )
                    )
            ),
            @ApiResponse(responseCode = "401", description = "Please login to access this resource.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Insufficient privileges to access this resource.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "User not found.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Map<String, Object>> getSavedBooks() {
        String userId = securityUtils.getCurrentUserId();
        logger.info("Fetching saved books for user: {}", userId);

        List<EmbeddedBookSavedDTO> response = userService.getSavedBooks(userId);

        logger.info("Successfully retrieved saved books for user: {}", userId);
        return ApiResponseUtil.ok(
                "Saved books retrieved successfully.",
                Map.of("savedBooks", response)
        );
    }

    /**
     * Retrieves the list of books read by the current user.
     *
     * @return A list of read books.
     */
    @GetMapping("/read")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Get read books", description = "Returns the list of books read by the current user.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved read books.",
                    content = @Content(
                            schema = @Schema(
                                    implementation = SuccessResponse.class
                            ),
                            examples = @ExampleObject(
                                    value = """
                                {
                                  "data": {
                                    "readBooks": [
                                      {
                                        "id": "679cb33db477993c5cdcdf9c",
                                        "title": "Der Tod in Venedig",
                                        "authors": [
                                          {
                                            "id": "679cb31ab477993c5cdc08da",
                                            "fullName": "Thomas Mann"
                                          }
                                        ],
                                        "libraryId": "679cb364d125ba32463b9746",
                                        "returnDate": "2025-02-09T16:55:26"
                                      },
                                      ...
                                    ]
                                  },
                                  "message": "Read books retrieved successfully.",
                                  "timestamp": "2025-02-09T17:58:55.1401252",
                                  "status": 200
                                }
                                """
                            )
                    )
            ),
            @ApiResponse(responseCode = "401", description = "Please login to access this resource.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Insufficient privileges to access this resource.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "User not found.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Map<String, Object>> getReadBooks() {
        String userId = securityUtils.getCurrentUserId();
        logger.info("Fetching read books for user: {}", userId);

        List<EmbeddedBookReadDTO> response = userService.getReadBooks(userId);

        logger.info("Successfully retrieved read books for user: {}", userId);
        return ApiResponseUtil.ok(
                "Read books retrieved successfully.",
                Map.of("readBooks", response)
        );
    }

    /**
     * Get the details of the current user.
     *
     * @return The user's personal details.
     */
    @GetMapping("/details")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Get user details", description = "Returns the personal details of the current user.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved user details.",
                    content = @Content(
                            schema = @Schema(
                                    implementation = SuccessResponse.class
                            ),
                            examples = @ExampleObject(
                                    value = """
                                {
                                  "data": {
                                    "userDetails": {
                                      "id": "67a5269e68b78d7479d391a6",
                                      "username": "john_doe",
                                      "name": "John",
                                      "surname": "Doe",
                                      "dateOfBirth": "1990-01-01",
                                      "email": "john.doe@example.com",
                                      "address": {
                                        "street": "Via Roma, 10",
                                        "city": "Firenze",
                                        "province": "FI",
                                        "postalCode": "50123",
                                        "country": "Italy"
                                      }
                                    }
                                  },
                                  "message": "User details retrieved successfully.",
                                  "timestamp": "2025-02-09T17:25:44.573562",
                                  "status": 200
                                }
                                """
                            )
                    )
            ),
            @ApiResponse(responseCode = "401", description = "Please login to access this resource.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Insufficient privileges to access this resource.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "User not found.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Map<String, Object>> getUserDetails() {
        String userId = securityUtils.getCurrentUserId();
        logger.info("Fetching user details for user ID: {}", userId);

        UserDetailsDTO userDetails = userService.getUserDetails(userId);

        logger.info("Successfully retrieved user details for user ID: {}", userId);
        return ApiResponseUtil.ok(
                "User details retrieved successfully.",
                Map.of("userDetails", userDetails)
        );
    }
}
