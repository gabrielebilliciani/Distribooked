package it.unipi.distribooked.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import it.unipi.distribooked.model.Reservation;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;

import java.time.LocalDateTime;

/**
 * Data Transfer Object (DTO) for reservation responses.
 *
 * Encapsulates the data required to represent a reservation.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReservationDTO {

    @Schema(description = "The ID of the user who made the reservation.", example = "63f2a1b7e4b0e74b5c9a5678")
    private String userId;

    @Schema(description = "The ID of the reserved book.", example = "63f2a1b7e4b0e74b5c9a9876")
    private String bookId;

    @Schema(description = "The ID of the library where the reservation was made.", example = "63f2a1b7e4b0e74b5c9a4567")
    private String libraryId;

    @Schema(description = "The timestamp of when the reservation was made.", example = "2025-01-23T15:30:00")
    private LocalDateTime reservedAt;

    @Schema(description = "The timestamp of when the reservation expires.", example = "2025-01-30T15:30:00")
    private LocalDateTime expiresAt;

}
