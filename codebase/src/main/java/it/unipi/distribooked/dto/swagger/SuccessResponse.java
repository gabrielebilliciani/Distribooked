package it.unipi.distribooked.dto.swagger;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Schema(name = "SuccessResponse", description = "Standard response format for successful operations (200 OK)")
public class SuccessResponse<T> {

    @Schema(description = "Timestamp of the response", example = "2025-01-31T12:00:00.000Z")
    private LocalDateTime timestamp;

    @Schema(description = "HTTP status code", example = "200")
    private int status;

    @Schema(description = "Success message", example = "Request completed successfully")
    private String message;

    @Schema(description = "Response data (varies depending on the endpoint)")
    private T data;

    public SuccessResponse(String message, T data) {
        this.timestamp = LocalDateTime.now();
        this.status = 200;
        this.message = message;
        this.data = data;
    }
}

