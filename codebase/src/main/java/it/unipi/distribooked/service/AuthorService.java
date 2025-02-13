package it.unipi.distribooked.service;

import it.unipi.distribooked.dto.AuthorDTO;
import it.unipi.distribooked.dto.AuthorSearchDTO;
import it.unipi.distribooked.mapper.AuthorMapper;
import it.unipi.distribooked.model.Author;
import it.unipi.distribooked.model.embedded.EmbeddedBookAuthor;
import it.unipi.distribooked.repository.mongo.AuthorRepository;
import it.unipi.distribooked.repository.mongo.views.AuthorSearchView;
import it.unipi.distribooked.utils.ObjectIdConverter;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@Service
public class AuthorService {

    @Autowired
    private AuthorRepository authorRepository;

    @Autowired
    private AuthorMapper authorMapper;

    /**
     * Searches for authors by full name and maps the results to `AuthorSearchDTO`.
     *
     * @param fullName The full name of the author to search for.
     * @return A list of authors matching the criteria.
     * @throws NoSuchElementException If no authors are found with the given name.
     */
    public List<AuthorSearchDTO> searchAuthorsByName(String fullName) {
        List<AuthorSearchView> authors = authorRepository.findAuthorsByFullName(fullName);

        if (authors.isEmpty()) {
            throw new NoSuchElementException("No authors found with the name: " + fullName);
        }

        // Map the results to DTOs using AuthorMapper
        return authors.stream()
                .map(authorMapper::toAuthorSearchDTO)
                .toList();
    }

    /**
     * Retrieves detailed information about an author by their ID.
     *
     * @param authorId The ID of the author to retrieve.
     * @return The details of the author as an `AuthorDTO`.
     * @throws IllegalArgumentException If the author ID format is invalid.
     * @throws NoSuchElementException If the author is not found.
     */
    public AuthorDTO getAuthorDetails(String authorId) {
        ObjectId objectId = ObjectIdConverter.convert(authorId); // Convert authorId to ObjectId

        // Retrieve the author from the repository
        Author author = authorRepository.findByIdWithLimitedBooks(objectId)
                .orElseThrow(() -> new NoSuchElementException("Author not found for ID: " + authorId));

        // Map the author to DTO using AuthorMapper
        return authorMapper.toAuthorDTO(author);
    }

    /**
     * Retrieves a paginated list of books written by an author.
     *
     * @param authorId The ID of the author.
     * @param page The page number (starting from 0).
     * @return A list of books written by the author.
     * @throws NoSuchElementException If the author is not found.
     */
    public List<EmbeddedBookAuthor> getAuthorBooks(String authorId, int page) {
        ObjectId objectId = convertToObjectId(authorId); // Convert authorId to ObjectId
        int skip = page * 20; // Calculate the number of documents to skip

        return authorRepository.findByIdWithBooksSlice(objectId, skip)
                .map(Author::getBooks)
                .orElseThrow(() -> new NoSuchElementException("Author not found for ID: " + authorId));
    }

    /**
     * Converts a string ID to ObjectId.
     *
     * @param id The string ID to convert.
     * @return The converted ObjectId.
     * @throws IllegalArgumentException If the ID format is invalid.
     */
    private ObjectId convertToObjectId(String id) {
        try {
            return new ObjectId(id);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid ID format: " + id, ex);
        }
    }
}