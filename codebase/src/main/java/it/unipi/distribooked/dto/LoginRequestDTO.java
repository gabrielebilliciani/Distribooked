package it.unipi.distribooked.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

/**
 * Data Transfer Object (DTO) for user login requests.
 *
 * This class encapsulates the data required for authenticating a user.
 * Lombok annotations are used to reduce boilerplate code.
 *
 * Lombok annotations:
 * - @Data: Generates getters, setters, toString, equals, and hashCode methods.
 * - @NoArgsConstructor: Generates a no-arguments constructor.
 * - @AllArgsConstructor: Generates an all-arguments constructor.
 * - @NonNull: Ensures non-null fields are checked during object creation.
 */
@Data
@NoArgsConstructor // Generates a no-arguments constructor
@AllArgsConstructor // Generates an all-arguments constructor
public class LoginRequestDTO {

    @Schema(description = "The username of the user attempting to log in.", example = "john_doe")
    @NonNull
    private String username; // The username of the user attempting to log in

    @Schema(description = "The password of the user attempting to log in.", example = "secureP@ssw0rd123")
    @NonNull
    private String password; // The password of the user attempting to log in (to be securely processed)
}
