package it.unipi.distribooked.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import it.unipi.distribooked.model.embedded.Address;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.geo.Point;

/**
 * Data Transfer Object (DTO) for library details.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LibraryDTO {

    @Schema(description = "The unique ID of the library.", example = "64b87f1a2d3b9c1234567890")
    private String id;

    @Schema(description = "The name of the library.", example = "Biblioteca degli Universitari Toscani")
    private String name;

    @Schema(description = "The address of the library.")
    private Address address;

    @Schema(description = "The district where the library is located.")
    private String district;

    @Schema(description = "The postal code of the library's location.", example = "12345")
    private String postalCode;

//    @Schema(description = "The municipality where the library is located.")
//    private String municipality;

//    @Schema(description = "The ISTAT code of the municipality.")
//    private String municipalityIstatCode;

    @Schema(description = "The province where the library is located.", example = "Province A")
    private String province;

    @Schema(description = "The region where the library is located.", example = "Region B")
    private String region;

    @Schema(description = "The ISTAT code of the province.", example = "654321")
    private String provinceIstatCode;

    @Schema(description = "The latitude coordinate of the library.", example = "43.7942")
    private Double latitude;

    @Schema(description = "The longitude coordinate of the library.", example = "11.2468")
    private Double longitude;

    @Schema(description = "The phone number of the library.", example = "123-456-7890")
    private String phone;

    @Schema(description = "The email address of the library.", example = "info@library.com")
    private String email;

    @Schema(description = "The website or catalog URL of the library.", example = "http://library.com")
    private String url;
}
