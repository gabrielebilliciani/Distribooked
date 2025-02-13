package it.unipi.distribooked.model;

import it.unipi.distribooked.model.embedded.Address;
import it.unipi.distribooked.model.embedded.EmbeddedBookRead;
import it.unipi.distribooked.model.embedded.EmbeddedBookSaved;
import it.unipi.distribooked.model.enums.UserType;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.util.List;

/**
 * Represents a User document in the MongoDB "users" collection.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "users") // Specifies the MongoDB collection
public class User {

    /*
     * @Field annotation can be used:
     * Specifies the name of the field in the MongoDB document if it differs from the field name in the Java class.
     * If the names are identical, this annotation is not necessary, as Spring automatically maps the Java field name
     * to the MongoDB field name.
     *
     * Example:
     * @Field("user_name") -> Maps the Java field 'username' to the MongoDB field 'user_name'.
     */

    /*
     * Index annotations (@Indexed, @GeoSpatialIndexed, etc.):
     * When added to a field, Spring Data MongoDB automatically manages the creation of the specified index in MongoDB.
     * During application startup, if the index does not exist in the collection, Spring creates it.
     * If the index already exists, Spring does nothing and uses the existing index.
     *
     * Example:
     * @Indexed -> Creates a regular index on the 'email' field.
     * @GeoSpatialIndexed -> Creates a geospatial index on the 'location' field.
     */

    @Id
    private ObjectId id; // MongoDB ID for the user

    @NonNull
    private String username; // The unique username of the user

    @NonNull
    private String name;

    @NonNull
    private String surname;

    @NonNull
    private LocalDate dateOfBirth; // The user's date of birth

    @NonNull
    private String password; // The hashed password of the user

    @NonNull
    private UserType userType; // The role of the user (e.g., ADMIN or USER)

    @NonNull
    private String email; // The email address of the user

    @NonNull
    private Address address; // Subdocument for detailed address

    private List<EmbeddedBookRead> readings; // List of books the user has read

    private double avgTravelDistance; // Average travel distance to borrow books

    private List<EmbeddedBookSaved> savedBooks; // List of saved books (max 50 items)
}
