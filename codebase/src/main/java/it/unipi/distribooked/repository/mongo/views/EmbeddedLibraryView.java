package it.unipi.distribooked.repository.mongo.views;

import it.unipi.distribooked.model.embedded.Address;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;

public interface EmbeddedLibraryView {
    ObjectId getId();
    String getName();
    GeoJsonPoint getLocation();
    Address getAddress();
    int getNumberOfCopies();
}
