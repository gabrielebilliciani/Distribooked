package it.unipi.distribooked.controller.secured;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import it.unipi.distribooked.dto.EmbeddedBookSavedDTO;
import it.unipi.distribooked.dto.ReservationDTO;
import it.unipi.distribooked.dto.swagger.CreatedResponse;
import it.unipi.distribooked.dto.swagger.ErrorResponse;
import it.unipi.distribooked.dto.swagger.SuccessResponse;
import it.unipi.distribooked.service.ReservationService;
import it.unipi.distribooked.service.UserService;
import it.unipi.distribooked.utils.ApiResponseUtil;
import it.unipi.distribooked.utils.SecurityUtils;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import it.unipi.distribooked.validation.ValidObjectId;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.Map;

/**
 * The ReservationController class manages reservation and book-saving-related secured endpoints.
 * It allows users to reserve and manage their book reservations and saved books.
 */
@RestController
@Tag(name = "Reservations", description = "Operations related to book reservations, for registered users.")
@RequestMapping("/api/v1/reservations")
@Validated
public class ReservationController {

    private static final Logger logger = LoggerFactory.getLogger(ReservationController.class);

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private UserService userService;

    @Autowired
    private SecurityUtils securityUtils;

    /**
     * Reserves a book for the current user in a specific library.
     *
     * @param bookId    The ID of the book to reserve.
     * @param libraryId The ID of the library where the reservation is made.
     * @return A ReservationDTO containing the details of the reservation.
     */
    @PostMapping("/reserve")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Reserve a book", description = "Allows a user to reserve a book in a specific library.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Book reserved successfully.",
                    content = @Content(
                            schema = @Schema(
                                    implementation = CreatedResponse.class
                            ),
                            examples = @ExampleObject(
                                    value = "{\n" +
                                            "  \"path\": \"/api/v1/reservations/reserve\",\n" +
                                            "  \"data\": {\n" +
                                            "    \"reservation\": {\n" +
                                            "      \"userId\": \"67a5269e68b78d7479d391a6\",\n" +
                                            "      \"bookId\": \"679cb34ab477993c5cdd4f0f\",\n" +
                                            "      \"libraryId\": \"679cb364d125ba32463b9759\",\n" +
                                            "      \"reservedAt\": \"2025-02-09T16:11:37.092\",\n" +
                                            "      \"expiresAt\": \"2025-02-09T16:15:56.292\"\n" +
                                            "    }\n" +
                                            "  },\n" +
                                            "  \"message\": \"Book reserved successfully.\",\n" +
                                            "  \"timestamp\": \"2025-02-09T17:11:37.1561781\",\n" +
                                            "  \"status\": 201\n" +
                                            "}"
                            )
                    )
            )
            ,
            @ApiResponse(responseCode = "400", description = "Invalid reservation details or max reservations reached.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Please login to access this resource.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Insufficient privileges to access this resource.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Book or library not found.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Reservation conflict.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Map<String, Object>> reserveBook(
            @Valid @ValidObjectId @RequestParam String bookId,
            @Valid @ValidObjectId @RequestParam String libraryId) {
        String userId = securityUtils.getCurrentUserId(); // Extract the user ID from the token
        logger.info("Reserving book for user ID: {}, book ID: {}, library ID: {}", userId, bookId, libraryId);

        ReservationDTO reservation = reservationService.reserveBook(userId, bookId, libraryId);

        logger.info("Book reserved successfully for user ID: {}, book ID: {}, library ID: {}", userId, bookId, libraryId);
        return ApiResponseUtil.created(
                "Book reserved successfully.",
                Map.of("reservation", reservation),
                "/api/v1/reservations/reserve"
        );
    }


    /**
     * Cancels a book reservation for the current user.
     *
     * @param bookId    The ID of the book to cancel.
     * @param libraryId The ID of the library where the book is located.
     * @return HTTP 200 OK if the cancellation is successful.
     */
    @DeleteMapping("/{bookId}/{libraryId}")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Cancel a book reservation", description = "Allows a user to cancel a book reservation.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Reservation cancelled successfully.",
                    content = @Content(schema = @Schema(implementation = SuccessResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid reservation details.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Please login to access this resource.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Insufficient privileges to access this resource.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Reservation not found.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Map<String, Object>> cancelReservation(
            @Valid @ValidObjectId @PathVariable String bookId,
            @Valid @ValidObjectId @PathVariable String libraryId) {

        String userId = securityUtils.getCurrentUserId();
        logger.info("Processing cancellation request: userId={}, bookId={}, libraryId={}", userId, bookId, libraryId);

        reservationService.cancelReservation(userId, bookId, libraryId);

        logger.info("Reservation cancelled successfully: userId={}, bookId={}, libraryId={}", userId, bookId, libraryId);
        return ApiResponseUtil.ok(
                "Reservation cancelled successfully.",
                null
        );
    }

    /**
     * Save a book for the current user.
     *
     * @param bookId The ID of the book to save.
     * @return The saved book's details.
     */
    @PostMapping("/save")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Save a book", description = "Allows a user to save a book for a maximum of 50.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Book saved successfully.",
                    content = @Content(
                            schema = @Schema(
                                    implementation = SuccessResponse.class
                            ),
                            examples = @ExampleObject(
                                    value = """
                                {
                                  "data": {
                                    "savedBook": {
                                      "id": "679cb34ab477993c5cdd4f0f",
                                      "title": "Triplanetary",
                                      "authors": [
                                        {
                                          "id": "679cb31ab477993c5cdc1bc7",
                                          "fullName": "E. E. (Edward Elmer) Smith"
                                        }
                                      ]
                                    }
                                  },
                                  "message": "Book saved successfully.",
                                  "timestamp": "2025-02-09T17:14:28.0451107",
                                  "status": 200
                                }
                                """
                            )
                    )
            ),
            @ApiResponse(responseCode = "400", description = "Book limit exceeded.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Please login to access this resource.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Insufficient privileges to access this resource.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Book not found.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Map<String, Object>> saveBook(@Valid @ValidObjectId @RequestParam String bookId) {
        String userId = securityUtils.getCurrentUserId(); // Extract the user ID from the token
        logger.info("Saving book for user ID: {}, book ID: {}", userId, bookId);

        EmbeddedBookSavedDTO savedBook = reservationService.saveBook(userId, bookId);

        logger.info("Book saved successfully for user ID: {}, book ID: {}", userId, bookId);

        return ApiResponseUtil.ok(
                "Book saved successfully.",
                Map.of("savedBook", savedBook)
        );
    }

    /**
     * Unsave a book for the current user.
     *
     * @param bookId The ID of the book to unsave.
     * @return HTTP 200 OK if the operation is successful.
     */
    @DeleteMapping("/unsave/{bookId}")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Unsave a book", description = "Removes a saved book from the user's list.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Book unsaved successfully.",
                    content = @Content(schema = @Schema(implementation = SuccessResponse.class))),
            @ApiResponse(responseCode = "401", description = "Please login to access this resource.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Insufficient privileges to access this resource.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Book not found in saved list.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Map<String, Object>> unsaveBook(@Valid @ValidObjectId @PathVariable String bookId) {
        String userId = securityUtils.getCurrentUserId(); // Extract the user ID from the security context

        logger.info("Unsaving book for user ID: {}", userId);
        reservationService.unsaveBook(userId, bookId);
        logger.info("Book unsaved successfully for user ID: {}", userId);

        return ApiResponseUtil.ok(
                "Book unsaved successfully.",
                null
        );
    }

}
