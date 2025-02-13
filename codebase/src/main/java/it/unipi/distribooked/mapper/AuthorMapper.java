package it.unipi.distribooked.mapper;

import it.unipi.distribooked.dto.AuthorDTO;
import it.unipi.distribooked.dto.AuthorSearchDTO;
import it.unipi.distribooked.model.Author;
import it.unipi.distribooked.repository.mongo.views.AuthorSearchView;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface AuthorMapper {

    @Mapping(target = "id", source = "id", qualifiedByName = "objectIdToString")
    @Mapping(target = "fullName", source = "fullName")
    @Mapping(target = "yearOfBirth", source = "yearOfBirth")
    @Mapping(target = "yearOfDeath", source = "yearOfDeath")
    AuthorSearchDTO toAuthorSearchDTO(AuthorSearchView author);

    @Mapping(target = "id", source = "id", qualifiedByName = "objectIdToString")
    AuthorDTO toAuthorDTO(Author author);

    @Named("objectIdToString")
    default String objectIdToString(org.bson.types.ObjectId id) {
        return id != null ? id.toString() : null;
    }
}