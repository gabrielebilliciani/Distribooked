package it.unipi.distribooked.model.embedded;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import it.unipi.distribooked.model.Author;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.bson.types.ObjectId;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Represents an embedded document for a book that a user has read.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmbeddedBookRead {

    @JsonSerialize(using = ToStringSerializer.class)
    private ObjectId id; // The unique identifier of the book

    private String title; // The title of the book

    private List<Author> authors; // The author of the book

    private ObjectId libraryId; // The ID of the library where the book was read

    private LocalDateTime returnDate; // The date when the book was returned
}
