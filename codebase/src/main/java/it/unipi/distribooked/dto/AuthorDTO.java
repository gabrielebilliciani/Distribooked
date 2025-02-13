package it.unipi.distribooked.dto;

import it.unipi.distribooked.model.embedded.EmbeddedBookAuthor;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Data Transfer Object (DTO) for detailed author information.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthorDTO {

    @Schema(description = "The unique ID of the author.", example = "64b87f1a2d3b9c1234567890")
    private String id;

    @Schema(description = "The full name of the author.", example = "Joshua Bloch")
    private String fullName;

    @Schema(description = "The year of birth of the author.", example = "1961")
    private String yearOfBirth;

    @Schema(description = "The year of death of the author, if applicable.", example = "2023")
    private String yearOfDeath;

    @Schema(description = "The URL of the author's avatar.", example = "https://example.com/avatar.jpg")
    private String avatarUrl;

    @Schema(description = "A short biography of the author.", example = "Joshua Bloch is a software engineer and author.")
    private String about;

    @Schema(description = "A list of books written by the author.")
    private List<EmbeddedBookAuthor> books;

}
