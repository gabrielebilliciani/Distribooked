package it.unipi.distribooked.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

/**
 * Data Transfer Object (DTO) for the registration response.
 *
 * This class encapsulates the data sent back to the client after a successful user registration.
 * It provides minimal information, such as the ID and username of the newly registered user,
 * to confirm that the operation was successful.
 *
 * Lombok annotations are used to automatically generate boilerplate code.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegistrationResponseDTO {

    @Schema(description = "The unique identifier of the newly registered user.", example = "64b87f1a2d3b9c1234567890")
    @NonNull
    private String id;

    @Schema(description = "The username of the newly registered user.", example = "john_doe")
    @NonNull
    private String username;
}
