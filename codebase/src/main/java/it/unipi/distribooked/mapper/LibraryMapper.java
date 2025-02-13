package it.unipi.distribooked.mapper;

import it.unipi.distribooked.dto.LibraryDTO;
import it.unipi.distribooked.model.Library;
import org.bson.types.ObjectId;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.springframework.data.geo.Point;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;

@Mapper(componentModel = "spring")
public interface LibraryMapper {

    @Mapping(target = "id", source = "id", qualifiedByName = "objectIdToString")
    @Mapping(target = "latitude", source = "location", qualifiedByName = "geoJsonPointToLatitude")
    @Mapping(target = "longitude", source = "location", qualifiedByName = "geoJsonPointToLongitude")
    LibraryDTO toLibraryDTO(Library library);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "location", source = ".", qualifiedByName = "coordinatesToGeoJsonPoint")
    Library toLibrary(LibraryDTO libraryDTO);

    @Named("objectIdToString")
    default String objectIdToString(ObjectId id) {
        return id != null ? id.toString() : null;
    }

    @Named("coordinatesToGeoJsonPoint")
    default GeoJsonPoint coordinatesToGeoJsonPoint(LibraryDTO dto) {
        if (dto.getLatitude() != null && dto.getLongitude() != null) {
            return new GeoJsonPoint(dto.getLongitude(), dto.getLatitude());
        }
        return null;
    }

    @Named("geoJsonPointToLatitude")
    default Double geoJsonPointToLatitude(GeoJsonPoint point) {
        return point != null ? point.getY() : null;
    }

    @Named("geoJsonPointToLongitude")
    default Double geoJsonPointToLongitude(GeoJsonPoint point) {
        return point != null ? point.getX() : null;
    }
}