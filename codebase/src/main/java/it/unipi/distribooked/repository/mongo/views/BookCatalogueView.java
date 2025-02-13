package it.unipi.distribooked.repository.mongo.views;

import it.unipi.distribooked.model.embedded.EmbeddedAuthor;
import org.bson.types.ObjectId;

import java.util.List;

/**
 * A projection interface for retrieving catalog-specific fields of a Book entity.
 * This is used to optimize database queries by fetching only the necessary fields required
 * for displaying books in the catalogue.
 */
public interface BookCatalogueView {
    ObjectId getId();
    String getTitle();
    String getSubtitle();
    List<EmbeddedAuthor> getAuthors();
    List<String> getCategories();
    String getCoverImageUrl();
}
