package it.unipi.distribooked.model;

import it.unipi.distribooked.model.embedded.EmbeddedBookAuthor;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

/**
 * Represents an Author document in the MongoDB "authors" collection.
 *
 * This class includes detailed metadata about the author and their books.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "authors") // Specifies the MongoDB collection name
public class Author {

    @Id
    private ObjectId id; // MongoDB ID for the author

    private String fullName; // Full name of the author

    private String yearOfBirth; // Author's date of birth

    private String yearOfDeath; // Optional: Author's date of death (null if still alive)

    private String avatarUrl; // URL to the author's avatar image

    private String about; // A short biography of the author

    private List<EmbeddedBookAuthor> books; // Embedded books written by the author

}
