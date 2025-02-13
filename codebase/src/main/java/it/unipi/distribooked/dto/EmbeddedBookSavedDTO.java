package it.unipi.distribooked.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import it.unipi.distribooked.model.embedded.EmbeddedAuthor;
import it.unipi.distribooked.model.embedded.EmbeddedBookSaved;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;

import java.util.List;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Details of a saved book.")
public class EmbeddedBookSavedDTO {

    @Schema(description = "The unique ID of the saved book.", example = "64c8f0d8e13d5c00234567ab")
    private String id;

    @Schema(description = "The title of the saved book.", example = "To Kill a Mockingbird")
    private String title;

    @Schema(description = "Authors of the saved book.", example = "[{\"name\":\"Harper Lee\"}]") // todo: fix this
    private List<EmbeddedAuthor> authors; // The authors of the book

}
