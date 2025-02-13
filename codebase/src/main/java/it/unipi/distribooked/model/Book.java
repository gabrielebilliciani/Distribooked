package it.unipi.distribooked.model;

import it.unipi.distribooked.model.embedded.EmbeddedAuthor;
import it.unipi.distribooked.model.embedded.EmbeddedLibrary;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

/**
 * Represents a Book document in the MongoDB "books" collection.
 *
 * This class includes detailed metadata about the book and its availability in libraries.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "books") // Specifies the MongoDB collection name
@CompoundIndexes({
        @CompoundIndex(
                name = "branches_location",
                def = "{'branches.location': '2dsphere'}"
        )
})
public class Book {

    @Id
    private ObjectId id; // MongoDB ID for the book

    private String title; // The title of the book

    private String subtitle; // Optional: The subtitle of the book

    private String publicationDate; // The publication date of the book

    private String language; // The language of the book (e.g., "English", "Italian")

    private List<String> categories; // Categories or genres of the book

    private String isbn10; // The 10-digit ISBN of the book

    private String isbn13; // The 13-digit ISBN of the book

    private String publisher; // The publisher of the book

    private String coverImageUrl; // Optional: The URL for the book's cover image

    private List<EmbeddedAuthor> authors; // List of authors associated with the book

    private List<EmbeddedLibrary> branches; // List of libraries where the book is available

    private int readingsCount = 0; // Total number of times the book has been read
}
