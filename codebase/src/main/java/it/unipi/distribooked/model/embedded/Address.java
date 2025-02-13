package it.unipi.distribooked.model.embedded;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.NonNull;

/**
 * Represents a detailed address for Italian formatting as a subdocument.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Address {

    @Schema(description = "Street name and house number.", example = "Via Roma, 10")
    @NonNull
    private String street; // Street name and house number

    @Schema(description = "City or comune of the address.", example = "Firenze")
    @NonNull
    private String city; // City or comune

    @Schema(description = "Province code (2 letters) of the address.", example = "FI")
    @NonNull
    private String province; // Italian province code (e.g., FI, MI)

    @Schema(description = "Postal code (CAP) for the address.", example = "50123")
    @NonNull
    private String postalCode; // CAP (Codice di Avviamento Postale)

    @Schema(description = "Country of the address.", example = "Italy")
    @NonNull
    private String country; // Country
}
