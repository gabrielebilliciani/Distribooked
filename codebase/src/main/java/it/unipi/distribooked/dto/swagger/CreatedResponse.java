package it.unipi.distribooked.dto.swagger;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Schema(name = "CreatedResponse", description = "Standard response format for successful resource creation (201 Created)")
public class CreatedResponse<T> {

    @Schema(description = "Timestamp of the response", example = "2025-01-31T12:00:00.000Z")
    private LocalDateTime timestamp;

    @Schema(description = "HTTP status code", example = "201")
    private int status;

    @Schema(description = "Success message", example = "Resource created successfully")
    private String message;

    @Schema(description = "Created resource data")
    private T data;

    @Schema(description = "Path of the newly created resource", example = "/api/v1/resource/123")
    private String path;

    public CreatedResponse(String message, T data, String path) {
        this.timestamp = LocalDateTime.now();
        this.status = 201;
        this.message = message;
        this.data = data;
        this.path = path;
    }
}

