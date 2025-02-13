package it.unipi.distribooked.repository.mongo.views;

import org.bson.types.ObjectId;

/**
 * A projection interface for retrieving summary information about authors.
 */
public interface AuthorSearchView {
    ObjectId getId();
    String getFullName();
    String getYearOfBirth();
    String getYearOfDeath();
}
