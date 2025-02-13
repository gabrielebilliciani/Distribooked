package it.unipi.distribooked.mapper;

import it.unipi.distribooked.dto.*;
import it.unipi.distribooked.model.embedded.Address;
import it.unipi.distribooked.model.embedded.EmbeddedBookRead;
import it.unipi.distribooked.model.embedded.EmbeddedBookSaved;
import it.unipi.distribooked.model.security.CustomUserDetails;
import it.unipi.distribooked.repository.mongo.views.UserAuthView;
import it.unipi.distribooked.repository.mongo.views.UserDetailsView;
import org.mapstruct.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collections;
import java.util.Collection;

/**
 * Mapper for converting between User entities, DTOs, and embedded objects.
 */
@Mapper(componentModel = "spring")
public interface UserMapper {

    default CustomUserDetails toCustomUserDetails(UserAuthView view) {
        if (view == null) {
            return null;
        }

        return new CustomUserDetails(
                view.getUsername(),
                view.getPassword(),
                view.getId(),
                Collections.singletonList(
                        new SimpleGrantedAuthority("ROLE_" + view.getUserType().name())
                )
        );
    }

    @Mapping(target = "address", source = "address")
    UserDetailsDTO toUserDetailsDTO(UserDetailsView view);

    AddressDTO toAddressDTO(Address address);

    @Mapping(target = "id", qualifiedByName = "objectIdToString")
    EmbeddedBookSavedDTO toEmbeddedBookSavedDTO(EmbeddedBookSaved book);

    @Mapping(target = "id", qualifiedByName = "objectIdToString")
    @Mapping(target = "libraryId", qualifiedByName = "objectIdToString")
    EmbeddedBookReadDTO toEmbeddedBookReadDTO(EmbeddedBookRead book);

    @Named("objectIdToString")
    default String objectIdToString(org.bson.types.ObjectId id) {
        return id != null ? id.toString() : null;
    }
}
