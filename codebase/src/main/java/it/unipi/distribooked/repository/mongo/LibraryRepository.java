package it.unipi.distribooked.repository.mongo;

import it.unipi.distribooked.model.Library;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import it.unipi.distribooked.repository.mongo.views.EmbeddedLibraryView;

import java.util.Optional;

/**
 * Repository interface for Library documents.
 */
@Repository
public interface LibraryRepository extends MongoRepository<Library, ObjectId> {
    // Spring Data provides default methods like findById(ObjectId)

    // @Query("{ 'name': ?0, 'postalCode': ?1 }")
    boolean existsByNameAndPostalCode(String name, String postalCode);

    @Query("{ '_id': ?0 }")
    @Field("libraryName: '$name'")
    Optional<EmbeddedLibraryView> findLibraryViewById(ObjectId libraryId);

    @Query(value = "{ '_id': ?0 }", fields = "{ 'name': 1, '_id': 0 }")
    String findNameById(String id);
}
