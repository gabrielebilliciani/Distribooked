package it.unipi.distribooked.model;

import it.unipi.distribooked.model.embedded.Address;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.geo.Point;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

/**
 * Represents a Library document in the MongoDB "libraries" collection.
 *
 * This class models the details of a library, including its location, administrative information, and optional contact details.
 */
@Data
@NoArgsConstructor // Generates a no-arguments constructor
@AllArgsConstructor // Generates an all-arguments constructor
@Document(collection = "branches") // Specifies the MongoDB collection name
public class Library {

    @Id
    private ObjectId id; // MongoDB unique identifier for the library

    @NonNull
    private String name; // The name of the library

    @NonNull
    private Address address; // The address of the library

    private String district; // Optional: The district where the library is located

    private String postalCode; // The postal code of the library's location

    private String municipality; // The municipality where the library is located

    private String municipalityIstatCode; // The ISTAT code of the municipality

    private String province; // The province where the library is located

    private String region; // The region where the library is located

    private String provinceIstatCode; // The ISTAT code of the province

    private GeoJsonPoint location; // The geographical coordinates of the library

    private String phone; // Optional: The phone number of the library

    private String email; // Optional: The email address of the library

    private String url; // Optional: The website or online catalog URL of the library
}
