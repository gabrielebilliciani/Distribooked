package it.unipi.distribooked.controller.open;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.unipi.distribooked.dto.LibraryDTO;
import it.unipi.distribooked.dto.swagger.ErrorResponse;
import it.unipi.distribooked.dto.swagger.SuccessResponse;
import it.unipi.distribooked.service.LibraryService;
import it.unipi.distribooked.utils.ApiResponseUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * The LibraryController class provides public endpoints for accessing library details.
 */
@RestController
@RequestMapping("/api/v1/libraries")
@Tag(name = "Library-related searches", description = "Endpoints available to everyone, without authentication.")
@Validated
public class LibraryController {

    private static final Logger logger = LoggerFactory.getLogger(LibraryController.class);

    @Autowired
    private LibraryService libraryService;

    /**
     * Get details of a specific library by its ID.
     *
     * This endpoint allows clients to retrieve detailed information about a specific library by providing its unique ID.
     * If the library does not exist, a 404 error is returned.
     *
     * @param id The unique ID of the library.
     * @return The details of the requested library, represented as `LibraryDTO`.
     */
    @Operation(summary = "Get library details",
            description = "Retrieve details of a specific library by its ID. Returns 404 if the library does not exist.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Library details retrieved successfully",
                    content = @Content(
                            schema = @Schema(
                                    implementation = SuccessResponse.class
                            ),
                            examples = @ExampleObject(
                                    value = """
                                {
                                  "data": {
                                    "id": "679cb364d125ba32463b9746",
                                    "name": "Biblioteca dell'Istituto Domus Mazziniana",
                                    "address": {
                                      "street": "Via Giuseppe Mazzini 71",
                                      "city": "Pisa",
                                      "province": "Pisa",
                                      "postalCode": "56125",
                                      "country": "Italia"
                                    },
                                    "latitude": 43.7114097,
                                    "longitude": 10.3980345,
                                    "phone": "+39 05024174",
                                    "email": "info@domusmazziniana.it",
                                    "url": "www.domusmazziniana.it"
                                  },
                                  "message": "Library details retrieved successfully",
                                  "timestamp": "2025-02-09T17:37:37.673684",
                                  "status": 200
                                }
                                """
                            )
                    )
            )
            ,
            @ApiResponse(responseCode = "404", description = "Library not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getLibraryById(
            @PathVariable String id) {

        logger.info("Fetching details for library with ID: {}", id);

        // Delegate logic to the service
        LibraryDTO library = libraryService.getLibraryById(id);

        logger.info("Library details retrieved for ID: {}", id);
        return ApiResponseUtil.ok("Library details retrieved successfully", library);
    }
}
