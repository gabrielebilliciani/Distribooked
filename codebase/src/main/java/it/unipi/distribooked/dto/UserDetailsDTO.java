package it.unipi.distribooked.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Details of a user's profile.")
public class UserDetailsDTO {

    @Schema(description = "The unique ID of the user.", example = "64c8f0d8e13d5c00234567ab")
    private String id;

    @Schema(description = "The username of the user.", example = "johndoe")
    private String username;

    @Schema(description = "The name of the user.", example = "John")
    private String name;

    @Schema(description = "The surname of the user.", example = "Doe")
    private String surname;

    @Schema(description = "The user's date of birth.", example = "1985-05-12")
    private LocalDate dateOfBirth;

    @Schema(description = "The email address of the user.", example = "johndoe@example.com")
    private String email;

    @Schema(description = "The address of the user.")
    private AddressDTO address;
}
