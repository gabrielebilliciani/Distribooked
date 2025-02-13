package it.unipi.distribooked.repository.mongo.views;

import org.bson.types.ObjectId;

public interface EmbeddedAuthorView {
    ObjectId getId();
    String getFullName();
}

