package it.unipi.distribooked.model.embedded;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;

import java.util.List;

/**
 * Represents an embedded Book in the Author document.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmbeddedBookAuthor {

    @JsonSerialize(using = ToStringSerializer.class)
    private ObjectId id; // MongoDB ID for the book

    private String title; // The title of the book

    private String subtitle;

    private List<EmbeddedAuthor> authors;

    private List<String> categories;

    private String coverImageUrl;
}
