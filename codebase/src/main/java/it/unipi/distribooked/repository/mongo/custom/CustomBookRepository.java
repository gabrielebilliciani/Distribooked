package it.unipi.distribooked.repository.mongo.custom;

import it.unipi.distribooked.dto.BookUtilizationDTO;
import it.unipi.distribooked.model.Book;
import org.bson.types.ObjectId;


import java.util.List;
import java.util.Map;

public interface CustomBookRepository {

    Book findBookWithNearbyLibraries(ObjectId bookId, double longitude, double latitude, Integer maxDistance);

    Map<String, List<BookUtilizationDTO>> findBooksUtilization();

}

