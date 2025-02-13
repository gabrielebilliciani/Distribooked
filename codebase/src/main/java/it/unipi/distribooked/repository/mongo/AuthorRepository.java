package it.unipi.distribooked.repository.mongo;

import it.unipi.distribooked.model.Author;
import it.unipi.distribooked.model.embedded.EmbeddedBookAuthor;
import it.unipi.distribooked.repository.mongo.custom.CustomAuthorRepository;
import it.unipi.distribooked.repository.mongo.views.AuthorSearchView;
import it.unipi.distribooked.repository.mongo.views.EmbeddedAuthorView;
import it.unipi.distribooked.repository.mongo.views.EmbeddedBookView;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AuthorRepository extends MongoRepository<Author, ObjectId>, CustomAuthorRepository {

    // Spring Data MongoDB will automatically handle the query for findById(ObjectId)

    /**
     * Retrieves an author by ID with a limited number of books (first 20 by default).
     *
     * @param id The author's ID.
     * @return The author document with a limited list of books.
     */
    @Query(value = "{ '_id': ?0 }", fields = "{ 'books': { $slice: 20 }, 'fullName': 1, 'yearOfBirth': 1, 'yearOfDeath': 1, 'avatarUrl': 1, 'about': 1, 'totalBooks': 1 }")
    Optional<Author> findByIdWithLimitedBooks(ObjectId id);

    /**
     * Retrieves an author by ID with books paginated using $slice.
     *
     * @param id   The author's ID.
     * @param skip The number of books to skip.
     * @return The author document with a limited list of books starting from the offset.
     */
    @Query(value = "{ '_id': ?0 }", fields = "{ 'books': { $slice: [ ?1, 20 ] }, 'fullName': 1, 'totalBooks': 1 }")
    Optional<Author> findByIdWithBooksSlice(ObjectId id, int skip);

//    @Query(value = "{ '_id': ?0 }", fields = "{ 'books': 1 }")
//    Page<EmbeddedBookAuthor> findBooksByAuthorId(ObjectId id, Pageable pageable);

    /**
     * Finds authors by their full name using projection.
     * <p>
     * Returns a list of authors represented as `AuthorSearchView`.
     */
    List<AuthorSearchView> findAuthorsByFullName(String fullName);

    @Query(value = "{ '_id': { $in: ?0 } }", fields = "{ 'fullName': 1 }")
    List<EmbeddedAuthorView> findAuthorsByIdsWithFields(List<ObjectId> ids);

    /**
     * Finds books by author name and book title using projection.
     * <p>
     * This query searches for authors with the given full name and then filters their embedded books
     * to return only those whose title matches the specified value. It uses projection to return
     * only the matched embedded books instead of full author documents.
     * <p>
     * This method uses a regex search on the embedded books' titles within each author document.
     * Since the scope of this query is limited to the books of a single author, the regex-based
     * filtering is manageable without significant performance concerns.
     * <p>
     * MongoDB efficiently optimizes this query if an index exists on `fullName`:
     * 1. The query first uses the index on `fullName` to quickly find matching author documents.
     * 2. For the matching documents, MongoDB scans the `books` array to find entries with a matching title.
     * 3. The `$elemMatch` operator ensures that only the books matching the criteria are returned,
     * reducing the amount of data transferred to the application.
     */
    @Query(value = "{ 'fullName': ?0, 'books.title': { $regex: ?1, $options: 'i' } }",
            fields = "{ 'books': { $elemMatch: { 'title': { $regex: ?1, $options: 'i' } } } }")
    Page<Author> findBooksByAuthorAndTitle(String fullName, String title, Pageable pageable);

}

