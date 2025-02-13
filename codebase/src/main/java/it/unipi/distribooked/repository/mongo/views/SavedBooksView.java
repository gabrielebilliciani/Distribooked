package it.unipi.distribooked.repository.mongo.views;

import it.unipi.distribooked.model.embedded.EmbeddedBookSaved;

import java.util.List;

public interface SavedBooksView {

    List<EmbeddedBookSaved> getSavedBooks();
}
