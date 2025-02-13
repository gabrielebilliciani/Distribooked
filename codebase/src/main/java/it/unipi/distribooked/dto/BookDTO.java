package it.unipi.distribooked.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import it.unipi.distribooked.model.embedded.EmbeddedAuthor;
import it.unipi.distribooked.model.embedded.EmbeddedLibrary;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Data Transfer Object (DTO) for detailed book information.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookDTO {

    @Schema(description = "The unique ID of the book.", example = "64b87f1a2d3b9c1234567890")
    private String id;

    @Schema(description = "The title of the book.", example = "Effective Java")
    private String title;

    @Schema(description = "The subtitle of the book, if any.", example = "Programming Best Practices")
    private String subtitle;

    @Schema(description = "The publication date of the book.", example = "2008-05-08")
    private String publicationDate;

    @Schema(description = "The language of the book.", example = "English")
    private String language;

    @Schema(description = "The categories or genres of the book.", example = "[\"Programming\", \"Java\"]")
    private List<String> categories;

    @Schema(description = "The 10-digit ISBN of the book.", example = "0134685997")
    private String isbn10;

    @Schema(description = "The 13-digit ISBN of the book.", example = "9780134685991")
    private String isbn13;

    @Schema(description = "The publisher of the book.", example = "Addison-Wesley")
    private String publisher;

    @Schema(description = "The URL of the book's cover image.", example = "https://example.com/effective-java.jpg")
    private String coverImageUrl;

    @Schema(description = "A list of authors associated with the book.")
    private List<EmbeddedAuthor> authors;

    @Schema(description = "A list of libraries where the book is available.")
    private List<EmbeddedLibraryDTO> branches;

    @Schema(description = "The total number of times the book has been read.", example = "120")
    private int readingsCount;
}
