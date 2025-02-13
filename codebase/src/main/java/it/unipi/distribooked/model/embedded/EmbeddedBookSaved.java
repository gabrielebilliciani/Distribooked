package it.unipi.distribooked.model.embedded;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.bson.types.ObjectId;

import java.util.List;

/**
 * Represents an embedded document for a book that a user has saved.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmbeddedBookSaved {

    @JsonSerialize(using = ToStringSerializer.class)
    private ObjectId id; // The unique identifier of the saved book

    private String title; // The title of the book

    private List<EmbeddedAuthor> authors; // The authors of the book
}
