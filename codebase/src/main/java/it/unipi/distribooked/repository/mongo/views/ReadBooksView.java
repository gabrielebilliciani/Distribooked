package it.unipi.distribooked.repository.mongo.views;

import it.unipi.distribooked.model.embedded.EmbeddedBookRead;

import java.util.List;

public interface ReadBooksView {

    List<EmbeddedBookRead> getReadings();
}
