package it.unipi.distribooked.model.enums;

/**
 * Enum representing the type of user in the system.
 *
 * Each user type has a pre-defined description that provides additional context about the role's purpose.
 * The description is assigned using the enum constructor, which is implicitly private and invoked automatically
 * for each enum constant.
 */
public enum UserType {

    /**
     * Administrator role.
     * Users with this role have full privileges and access to all functionalities of the system.
     */
    ADMIN("Administrator with full privileges"),

    /**
     * Regular user role.
     * Users with this role have limited access and can perform actions restricted to their account scope.
     */
    USER("Regular user with limited access");

    // Field to store the description of the user type
    private final String description;

    /**
     * Constructor to initialize the description of each enum constant.
     *
     * The constructor is implicitly private, meaning it cannot be called directly from outside the enum.
     * Java automatically invokes this constructor for each enum constant (e.g., ADMIN, USER),
     * passing the specified description during the declaration.
     *
     * @param description A human-readable description of the role.
     */
    UserType(String description) {
        this.description = description;
    }

    /**
     * Retrieves the description of the user type.
     *
     * @return The description associated with the user type.
     */
    public String getDescription() {
        return description;
    }
}
