package it.unipi.distribooked.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Data Transfer Object (DTO) for book catalogue responses.
 *
 * Encapsulates the data required for presenting a book in the catalogue.
 *
 * Lombok annotations are used to automatically generate boilerplate code.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookCatalogueDTO {

    private String id;

    @Schema(description = "Title of the book.", example = "Effective Java")
    private String title;

    @Schema(description = "Subtitle of the book, if any.", example = "Programming Language Guide")
    private String subtitle;

    @Schema(description = "List of authors of the book, including their IDs and names.")
    private List<EmbeddedAuthorDTO> authors;

    @Schema(description = "List of categories or genres of the book.", example = "[\"Programming\", \"Java\"]")
    private List<String> categories;

    @Schema(description = "URL of the cover image of the book.", example = "https://example.com/effective-java.jpg")
    private String coverImageUrl;
}
