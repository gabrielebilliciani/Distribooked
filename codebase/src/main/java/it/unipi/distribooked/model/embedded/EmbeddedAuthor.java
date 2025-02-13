package it.unipi.distribooked.model.embedded;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;

/**
 * Represents an embedded Author in the Book document.
 *
 * This class is used for embedding author details within a book document.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmbeddedAuthor {

    @JsonSerialize(using = ToStringSerializer.class)
    private ObjectId id; // ID of the author in the authors collection

    private String fullName; // Full name of the author
}
