package it.unipi.distribooked.controller.restricted;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.unipi.distribooked.dto.BookUtilizationDTO;
import it.unipi.distribooked.dto.BooksByAgeGroupDTO;
import it.unipi.distribooked.dto.swagger.ErrorResponse;
import it.unipi.distribooked.dto.swagger.SuccessResponse;
import it.unipi.distribooked.service.BookService;
import it.unipi.distribooked.service.StatisticsService;
import it.unipi.distribooked.utils.ApiResponseUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Slf4j
@Validated
@RestController
@RequestMapping("/api/v1/admin/statistics")
@Tag(name = "Usage statistics", description = "Operations related to statistics, for admins only.")
public class StatisticsController {

    @Autowired
    private StatisticsService statisticsService;

    @GetMapping("/most-read-by-age")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "(ADMIN) Get most read books by age group in a time interval",
            description = "Retrieves the most read books by different age groups within a specified time interval.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Statistics retrieved successfully",
                    content = @Content(
                            schema = @Schema(
                                    implementation = SuccessResponse.class
                            ),
                            examples = @ExampleObject(
                                    value = """
                                {
                                  "data": [
                                    {
                                      "ageGroup": "50+",
                                      "totalReadings": 3967,
                                      "mostReadBooks": [
                                        {
                                          "bookId": "679cb35fb477993c5cddede2",
                                          "bookTitle": "De Napoléon",
                                          "readCount": 3
                                        },
                                        ...
                                      ]
                                    },
                                    {
                                      "ageGroup": "30-49",
                                      "totalReadings": 1519,
                                      "mostReadBooks": [
                                        {
                                          "bookId": "679cb338b477993c5cdcb595",
                                          "bookTitle": "Riley Farm-Rhymes",
                                          "readCount": 2
                                        },
                                        ...
                                      ]
                                    },
                                    {
                                      "ageGroup": "18-29",
                                      "totalReadings": 962,
                                      "mostReadBooks": [
                                        {
                                          "bookId": "679cb363b477993c5cde0fb0",
                                          "bookTitle": "A vision of life",
                                          "readCount": 2
                                        },
                                        ...
                                      ]
                                    }
                                  ],
                                  "message": "Statistics retrieved successfully",
                                  "timestamp": "2025-02-09T17:41:05.8188524",
                                  "status": 200
                                }
                                """
                            )
                    )
            ),
            @ApiResponse(responseCode = "400", description = "Invalid date parameters",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Please login to access this resource.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Insufficient privileges to access this resource.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Map<String, Object>> getMostReadBooksByAgeGroup(
            @Parameter(description = "Start date (YYYY-MM-DD)", required = true)
            @RequestParam String startDate,
            @Parameter(description = "End date (YYYY-MM-DD)", required = true)
            @RequestParam String endDate) {

        log.info("Retrieving most read books by age group in the time interval {} - {}", startDate, endDate);
        List<BooksByAgeGroupDTO> stats = statisticsService.getMostReadBooksByAgeGroup(startDate, endDate);
        log.info("Most read books by age group retrieved successfully");
        return ApiResponseUtil.ok("Statistics retrieved successfully", stats);
    }

    @GetMapping("/average-age-readers")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "(ADMIN) Get the average age of active readers by geographical area",
            description = "Retrieves the average age of active readers (who borrowed at least one book in the last year) grouped by city.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Statistics retrieved successfully",
                    content = @Content(
                            schema = @Schema(
                                    implementation = SuccessResponse.class
                            ),
                            examples = @ExampleObject(
                                    value = """
                                {
                                  "data": {
                                    "Pisa": {
                                      "total_users": 697,
                                      "average_age": 59.76
                                    },
                                    "Livorno": {
                                      "total_users": 98,
                                      "average_age": 58.38
                                    },
                                    "Firenze": {
                                      "total_users": 93,
                                      "average_age": 56.98
                                    },
                                    "Lucca": {
                                      "total_users": 100,
                                      "average_age": 63.24
                                    }
                                  },
                                  "message": "Statistics retrieved successfully",
                                  "timestamp": "2025-02-09T17:44:26.2491445",
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
            @ApiResponse(responseCode = "500", description = "Internal server error.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Map<String, Object>> getAverageAgeOfReadersByCity() {
        log.info("Retrieving average age of active readers by city");
        Map<String, Map<String, Object>> stats = statisticsService.getAverageAgeOfReadersByCity();
        log.info("Average age of active readers by city retrieved successfully");
        return ApiResponseUtil.ok("Statistics retrieved successfully", stats);
    }

    @GetMapping("/books-utilization")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "(ADMIN) Get books utilization statistics",
            description = "Retrieves statistics on book utilization, including underutilized and overutilized books.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Statistics retrieved successfully",
                    content = @Content(
                            schema = @Schema(
                                    implementation = SuccessResponse.class
                            ),
                            examples = @ExampleObject(
                                    value = """
                                {
                                  "data": {
                                    "underutilized_books": [
                                      {
                                        "bookId": "679cb334b477993c5cdc8b4f",
                                        "title": "Alice's Adventures in Wonderland",
                                        "totalReadings": 0,
                                        "totalCopies": 13,
                                        "totalBranches": 4,
                                        "avgCopiesPerBranch": 3.25,
                                        "usageRatio": 0
                                      },
                                      ...
                                    ],
                                    "overutilized_books": [
                                      {
                                        "bookId": "679cb339b477993c5cdcbc25",
                                        "title": "An Outback Marriage: A Story of Australian Life",
                                        "totalReadings": 5,
                                        "totalCopies": 4,
                                        "totalBranches": 4,
                                        "avgCopiesPerBranch": 1,
                                        "usageRatio": 1.25
                                      },
                                      ...
                                    ]
                                  },
                                  "message": "Statistics retrieved successfully",
                                  "timestamp": "2025-02-09T17:43:08.6609359",
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
            @ApiResponse(responseCode = "500", description = "Internal server error.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Map<String, Object>> getBooksUtilization() {
        log.info("Retrieving books utilization statistics");
        Map<String, List<BookUtilizationDTO>> stats = statisticsService.getBooksUtilization();
        log.info("Books utilization statistics retrieved successfully");
        return ApiResponseUtil.ok("Statistics retrieved successfully", stats);
    }

}
