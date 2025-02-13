package it.unipi.distribooked.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import it.unipi.distribooked.model.embedded.EmbeddedAuthor;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;

/**
 * Data Transfer Object (DTO) for representing author information in book catalogue responses.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmbeddedAuthorDTO {

    @Schema(description = "ID of the author.", example = "64b87f1a2d3b9c1234567890")
    private String id;

    @Schema(description = "Full name of the author.", example = "Joshua")
    private String fullName;

}
