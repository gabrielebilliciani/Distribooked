package it.unipi.distribooked.repository.mongo;

import it.unipi.distribooked.model.Book;
import it.unipi.distribooked.model.embedded.EmbeddedLibrary;
import it.unipi.distribooked.repository.mongo.custom.CustomBookRepository;
import it.unipi.distribooked.repository.mongo.views.BookCatalogueView;
import it.unipi.distribooked.repository.mongo.views.LibraryCopiesView;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Update;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookRepository extends MongoRepository<Book, ObjectId>, CustomBookRepository {

    Page<BookCatalogueView> findAllBy(Pageable pageable);

    /**
     * Performs a strict search by the entire book title.
     *
     * Possible improvements: add support for partial matches, case-insensitive search, and other search criteria.
     *
     * This method requires an exact match for the title. This implementation ensures high performance
     * by leveraging indexed lookups on the `title` field, avoiding more costly operations like regex filtering.
     */
    Page<BookCatalogueView> findByTitle(String title, Pageable pageable);

    Optional<BookCatalogueView> findBookById(ObjectId id);

    Page<BookCatalogueView> findByCategories(String category, Pageable pageable);

    Page<BookCatalogueView> findAllByOrderByReadingsCountDesc(Pageable pageable);

    @Query("{ '_id': ?0, 'branches': { $elemMatch: { '_id': ?1, 'numberOfCopies': { $gt: 0 } } } }")
    @Update("{ '$inc': { 'branches.$.numberOfCopies': -1 } }")
    int decrementCopiesInLibraryIfAvailable(ObjectId bookId, ObjectId libraryId);

    @Query("{ '_id': ?0, 'branches.id': ?1 }")
    @Update("{ '$pull': { 'branches': { '_id': ?1 } } }")
    int removeLibraryFromBookIfPresent(ObjectId bookId, ObjectId libraryId);

    @Query(value = "{ '_id': ?0, 'branches._id': ?1 }", fields = "{ 'branches.$': 1 }")
    Optional<LibraryCopiesView> getCopiesInLibrary(ObjectId bookId, ObjectId libraryId);

//    @Query("{ '_id': ?0 }")
//    @Update("{ '$push': { 'branches': { 'id': ?1, 'numberOfCopies': 0 } } }")
//    void addLibraryToBook(ObjectId bookId, EmbeddedLibrary library);

    @Query(value = "{ '_id': ?0 }")
    @Update("{ '$push': { 'branches': ?1 } }")
    void addLibraryToBook(ObjectId bookId, EmbeddedLibrary library);

    @Query(value = "{ '_id': ?0, 'branches._id': ?1 }", exists = true)
    boolean existsLibraryInBook(ObjectId bookId, ObjectId libraryId);

    @Query("{ '_id': ?0, 'branches._id': ?1 }")
    @Update("{ '$inc': { 'branches.$.numberOfCopies': 1 } }")
    void incrementCopiesInLibrary(ObjectId bookId, ObjectId libraryId);

    @Query(value = "{ '_id': ?0 }", fields = "{ 'title': 1, '_id': 0 }")
    String findTitleById(String id);

    @Query("{ '_id': ?0 }")
    @Update("{ '$inc': { 'readingsCount': 1 } }")
    long incrementReadingsCount(ObjectId bookId);

    @Query(value = "{ 'categories': ?0 }", sort = "{ readingsCount: -1 }")
    Page<BookCatalogueView> findBooksByCategoryOrderByReadingsCount(String category, Pageable pageable);

    @Query(value = "{ 'title': ?0 }", collation = "{ 'locale': 'en', 'strength': 2, 'alternate': 'shifted' }")
    Page<BookCatalogueView> findByTitleWithCollation(String title, Pageable pageable);

}
