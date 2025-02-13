package it.unipi.distribooked.service;

import it.unipi.distribooked.dto.BookCatalogueDTO;
import it.unipi.distribooked.dto.BookDTO;
import it.unipi.distribooked.dto.BooksByAgeGroupDTO;
import it.unipi.distribooked.mapper.BookMapper;
import it.unipi.distribooked.model.Author;
import it.unipi.distribooked.model.Book;
import it.unipi.distribooked.repository.mongo.AuthorRepository;
import it.unipi.distribooked.repository.mongo.UserRepository;
import it.unipi.distribooked.repository.mongo.views.BookCatalogueView;
import it.unipi.distribooked.repository.mongo.views.EmbeddedBookView;
import it.unipi.distribooked.repository.redis.RedisBookRepository;
import it.unipi.distribooked.utils.ObjectIdConverter;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

/**
 * The BookService class handles the business logic related to books.
 * It provides methods for retrieving, searching, and managing book data.
 */
@Service
public class BookService {

    @Autowired
    private it.unipi.distribooked.repository.mongo.BookRepository bookRepository;

    @Autowired
    private AuthorRepository authorRepository;

    @Autowired
    private RedisBookRepository redisBookRepository;

    @Autowired
    private BookMapper bookMapper;

    @Autowired
    private UserRepository userRepository;

    /**
     * Retrieve a list of all books in the catalog.
     *
     * @return A paginated list of BookCatalogueDTO representing all books.
     */
    public Page<BookCatalogueDTO> getAllBooks(Pageable pageable) {
        // Use the projection to fetch only the required fields
        Page<BookCatalogueView> bookCatalogueViews = bookRepository.findAllBy(pageable);

        // Map the projection to DTOs
        return bookCatalogueViews.map(bookMapper::toBookCatalogueDTO);
    }

    public Page<BookCatalogueDTO> filterBooks(String category, Boolean sortByPopularity, Pageable pageable) {
        if (category != null && Boolean.TRUE.equals(sortByPopularity)) {
            return bookRepository.findBooksByCategoryOrderByReadingsCount(category, pageable)
                    .map(bookMapper::toBookCatalogueDTO);
        } else if (category != null) {
            return bookRepository.findByCategories(category, pageable)
                    .map(bookMapper::toBookCatalogueDTO);
        } else if (Boolean.TRUE.equals(sortByPopularity)) {
            return bookRepository.findAllByOrderByReadingsCountDesc(pageable)
                    .map(bookMapper::toBookCatalogueDTO);
        }

        throw new IllegalArgumentException("At least one filter must be specified");
    }

    /**
     * Search books by title and optionally by author.
     *
     * This method assumes that the dataset in the database is well-structured and does not contain duplicate
     * entries for books with the same title. This ensures that MongoDB will never need to transfer an excessive
     * number of documents over the network, making it unnecessary to implement additional checks or pagination
     * for title-based queries.
     *
     * This method provides flexible searching capabilities:
     * - If both `title` and `author` are provided, it searches for books written by the specified
     *   author and filters them by title.
     * - If only `title` is provided, it retrieves books matching the title.
     *
     * @param title  Mandatory filter for the book title.
     * @param author Optional filter for the author's name.
     * @return A list of books matching the search criteria, represented as `BookCatalogueDTO`.
     * @throws IllegalArgumentException if the title is not provided.
     */
    public Page<BookCatalogueDTO> searchBooks(String title, String author, int page) {
        Pageable pageable = PageRequest.of(page, 20);
        return (author != null)
                ? searchByAuthorAndTitle(author, title, pageable)
                : searchByTitle(title, pageable);
    }

    private Page<BookCatalogueDTO> searchByTitle(String title, Pageable pageable) {
        //Page<BookCatalogueView> bookPage = bookRepository.findByTitle(title, pageable);

        Page<BookCatalogueView> bookPage = bookRepository.findByTitleWithCollation(title, pageable);

        if (bookPage.isEmpty()) {
            throw new NoSuchElementException("No books found for title '" + title + "'.");
        }

        return bookPage.map(bookMapper::toBookCatalogueDTO);
    }

    private Page<BookCatalogueDTO> searchByAuthorAndTitle(String authorName, String title, Pageable pageable) {
        Page<Author> authorPage = authorRepository.findBooksByAuthorAndTitle(authorName, title, pageable);

        if (authorPage.isEmpty()) {
            throw new NoSuchElementException("No books found for author '" + authorName + "' and title '" + title + "'.");
        }

        // Manually extract and map matching books
        List<BookCatalogueDTO> matchingBooks = authorPage.stream()
                .flatMap(author -> author.getBooks().stream()
                        .filter(book -> book.getTitle().toLowerCase().contains(title.toLowerCase()))
                        .map(embeddedBook -> {
                            BookCatalogueDTO dto = new BookCatalogueDTO();
                            dto.setId(embeddedBook.getId() != null ? embeddedBook.getId().toString() : null);
                            dto.setTitle(embeddedBook.getTitle());
                            // Set other properties as needed
                            return dto;
                        }))
                .collect(Collectors.toList());

        return new PageImpl<>(matchingBooks, pageable, matchingBooks.size());
    }


    public BookDTO getBookByIdWithLibraries(String id, Double latitude, Double longitude, Integer radius) {
        ObjectId objectId = ObjectIdConverter.convert(id);
        Book book = getBookBasedOnGeospatialCriteria(objectId, latitude, longitude, radius);
        return bookMapper.toBookDTO(book);
    }

    private Book getBookBasedOnGeospatialCriteria(ObjectId objectId, Double latitude, Double longitude, Integer radius) {
        Book book;

        // Perform a single query depending on whether coordinates are provided
        if (latitude != null && longitude != null) {
            book = bookRepository.findBookWithNearbyLibraries(objectId, longitude, latitude, radius);
        } else {
            // Query the book without geospatial filtering
            book = bookRepository.findById(objectId)
                    .orElseThrow(() -> new NoSuchElementException("Book not found for ID"));
        }

        if (book == null) {
            throw new NoSuchElementException("Book not found for ID");
        }

        return book;
    }

    /**
     * Get the availability of a specific book in a specific library.
     *
     * @param bookId The ID of the book.
     * @param libraryId The ID of the library.
     * @return The number of available copies in the specified library.
     * @throws NoSuchElementException if the book or library is not found.
     */
    public Integer getBookAvailability(String bookId, String libraryId) {
        Integer availableCopies = redisBookRepository.getBookAvailabilityInLibrary(bookId, libraryId);

        if (availableCopies == null) {
            throw new NoSuchElementException("Book with ID " + bookId + " not found in library with ID " + libraryId);
        }

        return availableCopies;
    }


}
