package it.unipi.distribooked.service;

import it.unipi.distribooked.dto.LibraryDTO;
import it.unipi.distribooked.exceptions.DuplicateLibraryException;
import it.unipi.distribooked.mapper.LibraryMapper;
import it.unipi.distribooked.model.Library;
import it.unipi.distribooked.repository.mongo.LibraryRepository;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.data.geo.Point;

import java.util.NoSuchElementException;

/**
 * Service for managing library details.
 */
@Slf4j
@Service
public class LibraryService {

    @Autowired
    private LibraryRepository libraryRepository;

    @Autowired
    private LibraryMapper libraryMapper;

    /**
     * Retrieves library details by ID.
     *
     * @param id The ID of the library.
     * @return A DTO containing the library details.
     * @throws NoSuchElementException If no library is found for the given ID.
     */
    public LibraryDTO getLibraryById(String id) {
        ObjectId objectId = new ObjectId(id);

        // Retrieve the library document
        Library library = libraryRepository.findById(objectId)
                .orElseThrow(() -> new NoSuchElementException("Library not found for ID: " + id));

        // Map the Library document to LibraryDTO
        return libraryMapper.toLibraryDTO(library);

    }

    public void addLibrary(LibraryDTO libraryDTO) {

        Library library = libraryMapper.toLibrary(libraryDTO);

        // Check if a library with the same name and postalCode already exists
        boolean libraryExists = libraryRepository.existsByNameAndPostalCode(
                library.getName(),
                library.getPostalCode()
        );

        log.info("Library already exists: {}", libraryExists);

        if (libraryExists) {
            throw new DuplicateLibraryException("A library with the same name and postal code already exists.");
        }

        // Save the library if no duplicates found
        libraryRepository.save(library);
    }

}
