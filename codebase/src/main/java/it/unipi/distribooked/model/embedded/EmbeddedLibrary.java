package it.unipi.distribooked.model.embedded;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexed;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexType;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.bson.types.ObjectId;

import java.util.Map;

/**
 * Represents the availability of a book in a specific library.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmbeddedLibrary {

    @JsonSerialize(using = ToStringSerializer.class)
    private ObjectId id; // MongoDB unique identifier for the library

    private String libraryName; // Name of the library

    @GeoSpatialIndexed(type = GeoSpatialIndexType.GEO_2DSPHERE)
    private Map<String, Object> location;

    private Address address; // Physical address of the library

    private int numberOfCopies; // Number of copies available in the library
}
