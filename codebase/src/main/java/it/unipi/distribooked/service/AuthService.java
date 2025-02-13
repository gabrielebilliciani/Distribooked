package it.unipi.distribooked.service;

import it.unipi.distribooked.dto.RegistrationResponseDTO;
import it.unipi.distribooked.exceptions.UserAlreadyExistsException;
import it.unipi.distribooked.model.User;
import it.unipi.distribooked.model.embedded.Address;
import it.unipi.distribooked.model.enums.OutboxTaskType;
import it.unipi.distribooked.repository.mongo.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static it.unipi.distribooked.model.enums.UserType.USER;

/**
 * AuthService handles user authentication and registration functionalities.
 * This service communicates with the database and manages user-related actions,
 * such as creating a new user or validating login credentials.
 */
@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private OutboxService outboxService;

    /**
     * Registers a new user in the system.
     *
     * @param username    The username of the user to register.
     * @param password    The raw password of the user to register.
     * @param email       The email address of the user to register.
     * @param address     The address of the user to register.
     * @param name        The first name of the user to register.
     * @param surname     The last name of the user to register.
     * @param dateOfBirth The date of birth of the user to register.
     * @return A DTO containing the newly registered user's ID and username.
     */
    public RegistrationResponseDTO registerUser(String username, String password, String email, Address address,
                                                String name, String surname, LocalDate dateOfBirth) {
        logger.info("Processing registration request for user: {}", username);

        // Check if user already exists in the database
        if (userRepository.existsByUsername(username)) {
            // logger.warn("Registration failed: Username '{}' already exists", username); // already logged in GlobalExceptionHandler
            throw new UserAlreadyExistsException("User with username '" + username + "' already exists");
        }

        // Create a new User object and set its attributes
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password)); // Encode the raw password
        user.setUserType(USER);
        user.setEmail(email);
        user.setAddress(address);
        user.setName(name);
        user.setSurname(surname);
        user.setDateOfBirth(dateOfBirth);
        user.setAvgTravelDistance(0.0); // Initialize the average travel distance

        // Save the user in the database
        User savedUser = userRepository.save(user);
        logger.info("Successfully created new user account for: {}", username);

        // Convert the saved user entity to a DTO and return it
        return new RegistrationResponseDTO(savedUser.getId().toString(), savedUser.getUsername());
    }

    /**
     * Authenticates a user by validating their username and password.
     *
     * @param username The username of the user trying to authenticate.
     * @param password The raw password of the user trying to authenticate.
     * @return A JWT token if authentication is successful.
     */
    public String authenticateUser(String username, String password) {
        logger.info("Attempting to authenticate user: {}", username);

        // Perform authentication using the provided username and password
        // Delegate the authentication process to Spring Security's AuthenticationManager.
        // This method validates the provided username and password by:
        // 1. Calling the UserDetailsService to retrieve user information from the database.
        // 2. Verifying the password using the configured PasswordEncoder.
        // If authentication is successful, an Authentication object is returned, containing user details and authorities.
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password)
        );
        logger.info("User '{}' successfully authenticated", username);

        // Generate a JWT token for the authenticated user
        String token = tokenService.generateToken(authentication);
        logger.debug("Generated JWT token for user '{}'", username);

        return token;
    }

}
