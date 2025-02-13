package it.unipi.distribooked.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.unipi.distribooked.dto.*;
import it.unipi.distribooked.mapper.UserMapper;
import it.unipi.distribooked.model.User;
import it.unipi.distribooked.model.UserBookActivity;
import it.unipi.distribooked.model.embedded.Address;
import it.unipi.distribooked.model.embedded.EmbeddedAuthor;
import it.unipi.distribooked.model.embedded.EmbeddedBookRead;
import it.unipi.distribooked.model.embedded.EmbeddedBookSaved;
import it.unipi.distribooked.model.security.CustomUserDetails;
import it.unipi.distribooked.repository.mongo.BookRepository;
import it.unipi.distribooked.repository.mongo.UserRepository;
import it.unipi.distribooked.repository.mongo.views.*;
import it.unipi.distribooked.repository.redis.RedisUserRepository;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Service class responsible for user-related operations.
 *
 * Implements the Spring Security UserDetailsService interface to provide user authentication support.
 * This service retrieves user details from the database and maps them to Spring Security's UserDetails object.
 */
@Service
public class UserService implements UserDetailsService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RedisUserRepository redisUserRepository;

    /**
     * Loads a user's details by their username.
     *
     * This method is invoked by Spring Security during the authentication process. It retrieves the user
     * from the MongoDB database using the UserRepository and converts the data into a UserDetails object,
     * which is required by Spring Security.
     *
     * @param username The username to search for in the database.
     * @return A UserDetails object containing the user's credentials and authorities.
     * @throws UsernameNotFoundException If the user is not found in the database.
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        logger.info("Attempting to load user by username: {}", username);

        // Retrieve the user from the database
        UserAuthView user = userRepository.findByUsername(username);
        if (user == null) {
            // Log a warning and throw an exception if the user is not found
            logger.warn("User not found: {}", username);
            throw new UsernameNotFoundException("User not found"); // this exception is converted by Spring into BadCredentialsException
        }

        logger.info("User '{}' found in the database. Mapping to UserDetails.", username);

        return userMapper.toCustomUserDetails(user);

    }

    private String getAuthorsString(List<EmbeddedAuthor> authors) {
        if (authors == null || authors.isEmpty()) {
            return "Unknown Author";
        }

        StringBuilder authorString = new StringBuilder();
        for (EmbeddedAuthor author : authors) {
            authorString.append(author.getFullName()).append(", ");
        }

        // Remove the trailing comma and space
        return authorString.substring(0, authorString.length() - 2);
    }

    /**
     * Retrieves the list of saved books for a user.
     *
     * @param userId The ID of the user.
     * @return A list of saved books.
     */
    public List<EmbeddedBookSavedDTO> getSavedBooks(String userId) {
        SavedBooksView savedBooksView = userRepository.findSavedBooksById(new ObjectId(userId));
        if (savedBooksView == null) {
            throw new NoSuchElementException("User not found with ID: " + userId);
        }

        // Handle null savedBooks by returning an empty list
        List<EmbeddedBookSaved> savedBooks = savedBooksView.getSavedBooks();
        if (savedBooks == null) {
            return Collections.emptyList();
        }

        return savedBooks.stream()
                .map(userMapper::toEmbeddedBookSavedDTO)
                .toList();
    }

    /**
     * Retrieves the list of read books for a user.
     *
     * @param userId The ID of the user.
     * @return A list of read books.
     */
    public List<EmbeddedBookReadDTO> getReadBooks(String userId) {
        ReadBooksView readBooksView = userRepository.findReadBooksById(new ObjectId(userId));
        if (readBooksView == null) {
            throw new NoSuchElementException("User not found with ID: " + userId); // TODO change the message
        }

        // Handle null savedBooks by returning an empty list
        List<EmbeddedBookRead> readBooks = readBooksView.getReadings();
        if (readBooks == null) {
            return Collections.emptyList();
        }

        return readBooks.stream()
                .map(userMapper::toEmbeddedBookReadDTO)
                .toList();
    }

    /**
     * Get user details by ID.
     *
     * @param userId The ID of the user.
     * @return User details DTO.
     */
    public UserDetailsDTO getUserDetails(String userId) {
        UserDetailsView userDetailsView = userRepository.findUserDetailsById(new ObjectId(userId));
        if (userDetailsView == null) {
            throw new NoSuchElementException("User not found with ID: " + userId);
        }

        return userMapper.toUserDetailsDTO(userDetailsView);
    }

    public List<UserActiveBookDTO> getReservedAndLoanedBooks(String userId) {
        Map<String, UserBookActivity> activities = redisUserRepository.getUserBookActivities(userId);

        List<UserActiveBookDTO> result = new ArrayList<>();
        for (Map.Entry<String, UserBookActivity> entry : activities.entrySet()) {
            String libraryAndBookKey = entry.getKey(); // format "lib:LIBID:book:BOOKID"
            UserBookActivity activity = entry.getValue(); // contains book details

            // extract library and book IDs from the key
            String[] parts = libraryAndBookKey.split(":");
            String libraryId = parts[1];
            String bookId = parts[3];

            // logger.info("Found activity for book: {} in library: {}", bookId, libraryId);
            // logger.info("Activity: {}", activity);

            UserActiveBookDTO dto = new UserActiveBookDTO();
            dto.setBookId(bookId != null ? bookId : "null");
            dto.setLibraryId(libraryId != null ? libraryId : "null");
            dto.setLibraryName(activity != null && activity.getLibraryName() != null ? activity.getLibraryName() : "null");
            dto.setTitle(activity != null && activity.getTitle() != null ? activity.getTitle() : "null");
            dto.setStatus(activity != null && activity.getStatus() != null ? activity.getStatus().name() : "null");
            dto.setDeadlineDate(activity != null && activity.getDeadlineDate() != null ? activity.getDeadlineDate() : null);

            result.add(dto);
        }

        return result;
    }

}
