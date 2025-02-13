package it.unipi.distribooked.repository.mongo.views;

import it.unipi.distribooked.dto.EmbeddedAuthorDTO;
import it.unipi.distribooked.model.embedded.EmbeddedAuthor;
import org.bson.types.ObjectId;

import java.util.List;

public interface EmbeddedBookView {
    ObjectId getId();
    String getTitle();
    String getSubtitle();
    List<EmbeddedAuthor> getAuthors();
    List<String> getCategories();
    String getCoverImageUrl();
}
