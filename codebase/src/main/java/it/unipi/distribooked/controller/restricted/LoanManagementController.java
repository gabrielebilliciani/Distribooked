package it.unipi.distribooked.controller.restricted;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.unipi.distribooked.dto.*;
import it.unipi.distribooked.dto.swagger.ErrorResponse;
import it.unipi.distribooked.dto.swagger.SuccessResponse;
import it.unipi.distribooked.service.LoanManagementService;
import it.unipi.distribooked.service.UserService;
import it.unipi.distribooked.utils.ApiResponseUtil;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import it.unipi.distribooked.validation.ValidObjectId;

import java.util.List;
import java.util.Map;

/**
 * The LoanManagementController class manages administrative operations for loan management.
 * Provides endpoints for completing loans, viewing overdue loans, and retrieving all loans.
 */
@RestController
@RequestMapping("/api/v1/admin/loans")
@Tag(name = "Loans", description = "Operations related to loans, for admins only.")
@Validated
public class LoanManagementController {

    private static final Logger logger = LoggerFactory.getLogger(LoanManagementController.class);

    @Autowired
    private LoanManagementService loanManagementService;

    @Autowired
    private UserService userService;

    /**
     * Converts a reservation into a loan when a user picks up the reserved book.
     *
     * @param libraryId The ID of the library where the reservation exists.
     * @param userId    The ID of the user picking up the book.
     * @param bookId    The ID of the reserved book.
     * @return A response indicating success or failure.
     */
    @PostMapping("/{libraryId}/{userId}/{bookId}/mark-as-loan")
    @Operation(summary = "(ADMIN) Mark reservation as a loan",
            description = "Converts a reservation into a loan when the user picks up the reserved book.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Reservation successfully marked as loan.",
                    content = @Content(schema = @Schema(implementation = SuccessResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request data.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized access.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden – insufficient permissions.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Reservation not found.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Map<String, Object>> markAsLoan(
            @Valid @ValidObjectId @PathVariable String libraryId,
            @Valid @ValidObjectId @PathVariable String userId,
            @Valid @ValidObjectId @PathVariable String bookId) {
        logger.info("Marking reservation as loan: libraryId={}, userId={}, bookId={}", libraryId, userId, bookId);

        loanManagementService.markAsLoan(libraryId, userId, bookId);

        logger.info("Reservation successfully marked as loan: libraryId={}, userId={}, bookId={}", libraryId, userId, bookId);
        return ApiResponseUtil.ok(
                "Reservation successfully marked as loan.",
                null
        );
    }

    /**
     * Completes a loan when a user returns a borrowed book.
     *
     * @param libraryId The ID of the library where the book was loaned.
     * @param userId    The ID of the user returning the book.
     * @param bookId    The ID of the borrowed book.
     * @return A response indicating success or failure.
     */
    @PostMapping("/{libraryId}/{userId}/{bookId}/complete")
    @Operation(summary = "(ADMIN) Complete a loan",
            description = "Marks a loan as completed when a user returns a borrowed book, increasing availability.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Loan completed successfully.",
                    content = @Content(schema = @Schema(implementation = SuccessResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request data.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized access.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden – insufficient permissions.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Loan not found.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Map<String, Object>> completeLoan(
            @Valid @ValidObjectId @PathVariable String libraryId,
            @Valid @ValidObjectId @PathVariable String userId,
            @Valid @ValidObjectId @PathVariable String bookId) {
        logger.info("Completing loan: libraryId={}, userId={}, bookId={}", libraryId, userId, bookId);
        loanManagementService.completeLoan(libraryId, userId, bookId);
        logger.info("Loan completed successfully: libraryId={}, userId={}, bookId={}", libraryId, userId, bookId);

        return ApiResponseUtil.ok(
                "Loan completed successfully.",
                null
        );
    }

    /**
     * Retrieves a list of overdue loans for a specific library.
     *
     * @param libraryId The ID of the library to fetch overdue loans for.
     * @return A list of overdue loans (book IDs and user IDs).
     */
    @GetMapping("/{libraryId}/overdue")
    @Operation(summary = "(ADMIN) Retrieve overdue loans in a library",
            description = "Retrieves all overdue loans for a specific library.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Overdue loans retrieved successfully.",
                    content = @Content(
                            schema = @Schema(
                                    implementation = SuccessResponse.class
                            ),
                            examples = @ExampleObject(
                                    value = """
                                {
                                  "data": {
                                    "loans": [
                                      {
                                        "userId": "67a5269e68b78d7479d391a6",
                                        "bookId": "679cb33db477993c5cdcdf9c",
                                        "libraryId": "679cb364d125ba32463b9746",
                                        "loanedAt": "2025-02-09T17:51:11.904",
                                        "dueDate": "2025-03-11T17:51:11.9633414"
                                      }
                                    ]
                                  },
                                  "message": "Loans retrieved successfully.",
                                  "timestamp": "2025-02-09T17:51:41.6582558",
                                  "status": 200
                                }
                                """
                            )
                    )
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized access.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden – insufficient permissions.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Library not found.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Map<String, Object>> getOverdueLoans(@Valid @ValidObjectId @PathVariable String libraryId) {
        logger.info("Fetching overdue loans for library ID: {}", libraryId);
        List<OverdueLoanDTO> overdueLoans = loanManagementService.getOverdueLoans(libraryId);
        logger.info("Overdue loans retrieved successfully for library ID: {}", libraryId);

        return ApiResponseUtil.ok(
                "Overdue loans retrieved successfully.",
                Map.of("overdueLoans", overdueLoans)
        );
    }

    /**
     * Get all loans for a specific library.
     *
     * @param libraryId The ID of the library.
     * @return A list of all book IDs currently on loan.
     */
    @GetMapping("/{libraryId}/loans")
    @Operation(summary = "(ADMIN) Get all loans for a library",
            description = "Retrieves all active loans for a specific library.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Loans retrieved successfully.",
                    content = @Content(
                            schema = @Schema(
                                    implementation = SuccessResponse.class
                            ),
                            examples = @ExampleObject(
                                    value = """
                                {
                                  "data": {
                                    "loans": [
                                      {
                                        "userId": "67a5269e68b78d7479d391a6",
                                        "bookId": "679cb33db477993c5cdcdf9c",
                                        "libraryId": "679cb364d125ba32463b9746",
                                        "loanedAt": "2025-02-09T17:51:11.904",
                                        "dueDate": "2025-03-11T17:51:11.9633414"
                                      },
                                      ...
                                    ]
                                  },
                                  "message": "Loans retrieved successfully.",
                                  "timestamp": "2025-02-09T17:51:41.6582558",
                                  "status": 200
                                }
                                """
                            )
                    )
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized access.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden – insufficient permissions.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Library not found.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Map<String, Object>> getAllLoans(@Valid @ValidObjectId @PathVariable String libraryId) {
        logger.info("Fetching all loans for library ID: {}", libraryId);
        List<LoanDTO> loans = loanManagementService.getAllLoans(libraryId);
        logger.info("Loans retrieved successfully for library ID: {}", libraryId);

        return ApiResponseUtil.ok(
                "Loans retrieved successfully.",
                Map.of("loans", loans)
        );
    }

    /**
     * Get all reservations for a specific library.
     *
     * @param libraryId The ID of the library.
     * @return A list of all book IDs currently reserved.
     */
    @GetMapping("/{libraryId}/reservations")
    @Operation(summary = "(ADMIN) Get all reservations for a library",
            description = "Retrieves all active reservations for a specific library.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Reservations retrieved successfully.",
                    content = @Content(
                            schema = @Schema(
                                    implementation = SuccessResponse.class
                            ),
                            examples = @ExampleObject(
                                    value = """
                                {
                                  "data": {
                                    "reservations": [
                                      {
                                        "userId": "67a5269e68b78d7479d391a6",
                                        "bookId": "679cb33db477993c5cdcdf9c",
                                        "libraryId": "679cb364d125ba32463b9746",
                                        "reservedAt": "2025-02-09T17:48:41.337",
                                        "expiresAt": "2025-02-12T17:48:41.4001696"
                                      },
                                      ...
                                    ]
                                  },
                                  "message": "Reservations retrieved successfully.",
                                  "timestamp": "2025-02-09T17:49:31.2201696",
                                  "status": 200
                                }
                                """
                            )
                    )
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized access.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden – insufficient permissions.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Library not found.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Map<String, Object>> getAllReservations(@Valid @ValidObjectId @PathVariable String libraryId) {
        logger.info("Fetching all reservations for library ID: {}", libraryId);

        List<ReservationDTO> reservations = loanManagementService.getAllReservations(libraryId);

        logger.info("Reservations retrieved successfully for library ID: {}", libraryId);
        return ApiResponseUtil.ok(
                "Reservations retrieved successfully.",
                Map.of("reservations", reservations)
        );
    }

    /**
     * Retrieves the list of books reserved and loaned by a user.
     *
     * @param userId The ID of the user.
     * @return A list of reserved and loaned books.
     */
    @GetMapping("/{id}/reserved-loaned")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "(ADMIN) Get reserved and loaned books",
            description = "Retrieves a list of books currently reserved or loaned by the specified user.")
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
            @ApiResponse(responseCode = "401", description = "Unauthorized access – authentication required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden – insufficient permissions",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "User not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Map<String, Object>> getUserReservedAndLoanedBooks(@Valid @ValidObjectId @PathVariable("id") String userId) {

        logger.info("Fetching reserved and loaned books for user: {}", userId);

        List<UserActiveBookDTO> reservedAndLoanedBooks = userService.getReservedAndLoanedBooks(userId);

        logger.info("Successfully retrieved reserved and loaned books for user: {}", userId);

        return ApiResponseUtil.ok(
                "Reserved and loaned books retrieved successfully.",
                Map.of("reservedAndLoanedBooks", reservedAndLoanedBooks)
        );
    }

    /**
     * Get user details.
     *
     * @param userId The ID of the user.
     * @return The user's personal details.
     */
    @GetMapping("/{id}/details")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "(ADMIN) Get user details",
            description = "Retrieves the personal details of a user.")
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
            @ApiResponse(responseCode = "401", description = "Unauthorized access – authentication required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden – insufficient permissions",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "User not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Map<String, Object>> getUserDetails(@Valid @ValidObjectId @PathVariable("id") String userId) {

        logger.info("Fetching user details for user ID: {}", userId);

        UserDetailsDTO userDetails = userService.getUserDetails(userId);

        logger.info("Successfully retrieved user details for user ID: {}", userId);
        return ApiResponseUtil.ok(
                "User details retrieved successfully.",
                Map.of("userDetails", userDetails)
        );
    }

}
