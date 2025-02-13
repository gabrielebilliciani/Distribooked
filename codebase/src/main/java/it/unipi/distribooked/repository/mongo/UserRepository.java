package it.unipi.distribooked.repository.mongo;

import it.unipi.distribooked.model.User;
import it.unipi.distribooked.repository.mongo.custom.CustomUserRepository;
import it.unipi.distribooked.repository.mongo.views.ReadBooksView;
import it.unipi.distribooked.repository.mongo.views.SavedBooksView;
import it.unipi.distribooked.repository.mongo.views.UserAuthView;
import it.unipi.distribooked.repository.mongo.views.UserDetailsView;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for accessing User documents in MongoDB.
 * Extends MongoRepository to leverage built-in CRUD operations.
 */
@Repository
public interface UserRepository extends MongoRepository<User, ObjectId>, CustomUserRepository {

    // save method is inherited from MongoRepository

    /**
     * This method utilizes the UserAuthView projection interface to optimize the query.
     * Instead of retrieving the entire User document from the database, only the fields
     * defined in UserAuthView (username, password, userType) are fetched.
     */
    UserAuthView findByUsername(String username);

    // Retrieve saved books using SavedBooksView projection
    SavedBooksView findSavedBooksById(ObjectId id);

    // Retrieve read books using ReadBooksView projection
    ReadBooksView findReadBooksById(ObjectId id);

    boolean existsByUsername(String username);

    // Retrieve user details using projection
    UserDetailsView findUserDetailsById(ObjectId id);


}
