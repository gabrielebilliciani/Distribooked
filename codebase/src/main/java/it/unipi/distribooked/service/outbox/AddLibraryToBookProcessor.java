package it.unipi.distribooked.service.outbox;

import it.unipi.distribooked.model.OutboxTask;
import it.unipi.distribooked.model.embedded.EmbeddedLibrary;
import it.unipi.distribooked.repository.mongo.BookRepository;
import it.unipi.distribooked.repository.mongo.LibraryRepository;
import it.unipi.distribooked.repository.mongo.views.EmbeddedLibraryView;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@Slf4j
@Component("ADD_LIBRARY_TO_BOOK")
public class AddLibraryToBookProcessor implements OutboxTaskProcessor {

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private LibraryRepository libraryRepository;

    @Override
    public void process(OutboxTask task) {
        String bookId = (String) task.getPayload().get("bookId");
        String libraryId = (String) task.getPayload().get("libraryId");
        Integer initialValue = (Integer) task.getPayload().get("initialValue");

        ObjectId bookObjectId = new ObjectId(bookId);
        ObjectId libraryObjectId = new ObjectId(libraryId);

        // Check if library already exists in book
        // this makes the processor idempotent
        if (bookRepository.existsLibraryInBook(bookObjectId, libraryObjectId)) {
            log.warn("Library {} already exists in book {}, skipping addition", libraryId, bookId);
            return;
        }

        EmbeddedLibraryView libraryView = libraryRepository.findLibraryViewById(new ObjectId(libraryId))
                .orElseThrow(() -> new NoSuchElementException("Library not found with id: " + libraryId));

        EmbeddedLibrary embeddedLibrary = new EmbeddedLibrary();
        embeddedLibrary.setId(libraryView.getId());
        embeddedLibrary.setLibraryName(libraryView.getName());
        Map<String, Object> geoJsonPoint = Map.of(
                "type", "Point",
                "coordinates", List.of(libraryView.getLocation().getX(), libraryView.getLocation().getY()) // X = longitude, Y = latitude
        );
        embeddedLibrary.setLocation(geoJsonPoint);
        embeddedLibrary.setAddress(libraryView.getAddress());
        embeddedLibrary.setNumberOfCopies(initialValue != null ? initialValue : 0);

        bookRepository.addLibraryToBook(new ObjectId(bookId), embeddedLibrary);

        log.info("Added library {} to book {}", libraryId, bookId);
    }
}

