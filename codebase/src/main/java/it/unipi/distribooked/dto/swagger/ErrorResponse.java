package it.unipi.distribooked.dto.swagger;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(name = "ErrorResponse", description = "Standard error response format")
public class ErrorResponse {
    @Schema(description = "Type of error")
    private String type;

    @Schema(description = "Error title")
    private String title;

    @Schema(description = "HTTP status code", example = "4xx, 5xx")
    private int status;

    @Schema(description = "Error details")
    private String detail;

    @Schema(description = "Instance of the request that caused the error", example = "/api/v1/resource/{id}")
    private String instance;

    @Schema(description = "Timestamp of the error occurrence", example = "1738325243029")
    private long timestamp;

    @Schema(description = "Additional error information")
    private String error;
}

