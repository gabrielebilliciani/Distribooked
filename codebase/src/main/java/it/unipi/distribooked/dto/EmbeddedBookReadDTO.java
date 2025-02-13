package it.unipi.distribooked.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import it.unipi.distribooked.model.embedded.EmbeddedAuthor;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Details of a read book.")
public class EmbeddedBookReadDTO {

    @Schema(description = "The unique ID of the read book.", example = "64c8f0d8e13d5c00234567ab")
    private String id;

    @Schema(description = "The title of the read book.", example = "To Kill a Mockingbird")
    private String title;

    @Schema(description = "Authors of the saved book.", example = "[{\"name\":\"Harper Lee\"}]") // todo: fix this
    private List<EmbeddedAuthor> authors; // The authors of the book

    @Schema(description = "The ID of the library where the book was read.", example = "5f8f0d8e13d5c00234567ab")
    private String libraryId;

    @Schema(description = "The date when the book was returned.", example = "2023-10-15T10:30:00")
    private LocalDateTime returnDate;
}
