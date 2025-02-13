package it.unipi.distribooked.mapper;

import it.unipi.distribooked.dto.*;
import it.unipi.distribooked.model.Book;
import it.unipi.distribooked.model.embedded.*;
import it.unipi.distribooked.repository.mongo.views.BookCatalogueView;
import it.unipi.distribooked.repository.mongo.views.EmbeddedBookView;
import org.bson.types.ObjectId;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring", uses = {LibraryMapper.class})
public interface BookMapper {

    @Mapping(target = "id", source = "id", qualifiedByName = "bookObjectIdToString")
    BookCatalogueDTO toBookCatalogueDTO(BookCatalogueView view);

    @Mapping(target = "id", source = "id", qualifiedByName = "bookObjectIdToString")
    BookCatalogueDTO toBookCatalogueDTO(EmbeddedBookView view);

    @Mapping(target = "id", source = "id", qualifiedByName = "bookObjectIdToString")
    @Mapping(target = "branches", source = "branches", qualifiedByName = "embeddedLibraryToDTO")
    BookDTO toBookDTO(Book book);

    @Mapping(target = "id", source = "id", qualifiedByName = "bookObjectIdToString")
    EmbeddedBookSavedDTO toEmbeddedBookSavedDTO(EmbeddedBookSaved book);

    @Mapping(target = "id", source = "id", qualifiedByName = "bookObjectIdToString")
    EmbeddedAuthorDTO toEmbeddedAuthorDTO(EmbeddedAuthor author);

    @Mapping(target = "id", source = "id", qualifiedByName = "bookObjectIdToString")
    @Mapping(target = "libraryId", source = "libraryId", qualifiedByName = "bookObjectIdToString")
    EmbeddedBookReadDTO toEmbeddedBookReadDTO(EmbeddedBookRead book);

    @Mapping(target = "id", expression = "java(dto.getId() == null ? null : new org.bson.types.ObjectId(dto.getId()))")
    @Mapping(target = "branches", source = "branches", qualifiedByName = "dtoToEmbeddedLibrary")
    Book toBook(BookDTO dto);

    @Mapping(target = "id", source = "id")
    EmbeddedBookAuthor toEmbeddedBookAuthor(Book book);

    @Named("bookObjectIdToString")
    default String objectIdToString(org.bson.types.ObjectId id) {
        return id != null ? id.toString() : null;
    }

    @Named("addressToString")
    default String addressToString(Address address) {
        if (address == null) return null;
        return String.format("%s, %s, %s %s, %s",
                address.getStreet(),
                address.getCity(),
                address.getProvince(),
                address.getPostalCode(),
                address.getCountry());
    }

    @Named("embeddedLibraryToDTO")
    default EmbeddedLibraryDTO embeddedLibraryToDTO(EmbeddedLibrary library) {
        if (library == null) {
            return null;
        }

        double[] coordinates = null;
        if (library.getLocation() != null && library.getLocation().get("coordinates") instanceof List<?>) {
            @SuppressWarnings("unchecked")
            List<Number> coords = (List<Number>) library.getLocation().get("coordinates");
            if (coords != null && coords.size() >= 2) {
                coordinates = new double[]{coords.get(0).doubleValue(), coords.get(1).doubleValue()};
            }
        }

        return new EmbeddedLibraryDTO(
                objectIdToString(library.getId()),
                library.getLibraryName(),
                coordinates,
                addressToString(library.getAddress()),
                library.getNumberOfCopies()
        );
    }

    @Named("dtoToEmbeddedLibrary")
    default EmbeddedLibrary dtoToEmbeddedLibrary(EmbeddedLibraryDTO dto) {
        if (dto == null) {
            return null;
        }

        Map<String, Object> location = null;
        if (dto.getCoordinates() != null && dto.getCoordinates().length >= 2) {
            location = new HashMap<>();
            location.put("type", "Point");
            location.put("coordinates", Arrays.asList(dto.getCoordinates()[0], dto.getCoordinates()[1]));
        }

        String[] addressParts = dto.getAddress() != null ? dto.getAddress().split(", ") : null;
        Address address = null;
        if (addressParts != null && addressParts.length >= 5) {
            address = new Address(
                    addressParts[0],                    // street
                    addressParts[1],                    // city
                    addressParts[2],                    // province
                    addressParts[3],                    // postalCode
                    addressParts[4]                     // country
            );
        }

        return new EmbeddedLibrary(
                dto.getId() != null ? new ObjectId(dto.getId()) : null,
                dto.getLibraryName(),
                location,
                address,
                dto.getNumberOfCopies()
        );
    }
}