package it.unipi.distribooked.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for Embedded Library details.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmbeddedLibraryDTO {

    @Schema(description = "The unique ID of the library.", example = "64b87f1a2d3b9c1234567890")
    private String id;

    @Schema(description = "The name of the library.", example = "Central Library")
    private String libraryName;

    @Schema(description = "The geographical coordinates of the library.", example = "[45.4642, 9.1900]")
    private double[] coordinates;

    @Schema(description = "The address of the library.", example = "123 Main St, Springfield")
    private String address;

    @Schema(description = "The number of copies available.", example = "5")
    private int numberOfCopies;
}
