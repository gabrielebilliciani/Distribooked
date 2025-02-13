package it.unipi.distribooked.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Detailed address of the user.")
public class AddressDTO {

    @Schema(description = "Street name and house number.", example = "Via Roma, 10")
    private String street;

    @Schema(description = "City or comune of the address.", example = "Firenze")
    private String city;

    @Schema(description = "Province code (2 letters) of the address.", example = "FI")
    private String province;

    @Schema(description = "Postal code (CAP) for the address.", example = "50123")
    private String postalCode;

    @Schema(description = "Country of the address.", example = "Italy")
    private String country;
}