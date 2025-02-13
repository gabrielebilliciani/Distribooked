package it.unipi.distribooked.repository.mongo.views;

import it.unipi.distribooked.model.enums.UserType;

/**
 * A projection interface for retrieving authentication-specific fields of a User entity.
 * This is used to optimize database queries by fetching only the necessary fields required
 * for user authentication, rather than the entire User document.
 */
public interface UserAuthView {

    /**
     * Retrieves the unique identifier of the user.
     * This field is used to uniquely identify the user in the system.
     *
     * @return The unique identifier as a String.
     */
    String getId();

    /**
     * Retrieves the username of the user.
     * This field is used as the unique identifier for user authentication.
     *
     * @return The username as a String.
     */
    String getUsername();

    /**
     * Retrieves the hashed password of the user.
     * This field is used to validate the user's credentials during the authentication process.
     *
     * @return The hashed password as a String.
     */
    String getPassword();

    /**
     * Retrieves the user type of the user.
     * The user type determines the role or permissions assigned to the user in the system.
     * Typical examples include "ADMIN" or "USER".
     *
     * @return The UserType enumeration value representing the user's role.
     */
    UserType getUserType();
}

