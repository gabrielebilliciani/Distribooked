package it.unipi.distribooked.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object (DTO) for author search results.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthorSearchDTO {

    @Schema(description = "The unique ID of the author.", example = "64b87f1a2d3b9c1234567890")
    private String id;

    @Schema(description = "The full name of the author.", example = "Joshua Bloch")
    private String fullName;

    @Schema(description = "The date of birth of the author.", example = "1961-08-28")
    private String yearOfBirth;

    @Schema(description = "The date of death of the author, if applicable.", example = "2023-01-01")
    private String yearOfDeath;
}
