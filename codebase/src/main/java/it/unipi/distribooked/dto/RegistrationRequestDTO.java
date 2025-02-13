package it.unipi.distribooked.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import it.unipi.distribooked.model.embedded.Address;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Data Transfer Object (DTO) for user registration requests.
 *
 * Encapsulates the data required for registering a new user.
 *
 * Lombok annotations are used to automatically generate boilerplate code.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegistrationRequestDTO {

    @Schema(description = "Unique username for the user, must be 3-30 characters.", example = "john_doe")
    @NonNull
    @Size(min = 3, max = 30, message = "Username must be between 3 and 30 characters.")
    private String username;

    @Schema(description = "Password for the user, must be 8-50 characters.", example = "P@ssword123")
    @NonNull
    @Size(min = 8, max = 50, message = "Password must be between 8 and 50 characters.")
    private String password;

    @Schema(description = "User's email address.", example = "john.doe@example.com")
    @NonNull
    @Email(message = "Invalid email format.")
    private String email;

    @Schema(description = "User's detailed address.")
    @NonNull
    @Valid
    private Address address;

    @Schema(description = "User's first name, must be 2-50 characters.", example = "John")
    @NonNull
    @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters.")
    private String name;

    @Schema(description = "User's last name, must be 2-50 characters.", example = "Doe")
    @NonNull
    @Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters.")
    private String surname;

    @Schema(description = "User's date of birth, must be in the past.", example = "1990-01-01")
    @NonNull
    @NotNull(message = "Date of birth is required.")
    @Past(message = "Date of birth must be in the past.")
    private LocalDate dateOfBirth;
}

