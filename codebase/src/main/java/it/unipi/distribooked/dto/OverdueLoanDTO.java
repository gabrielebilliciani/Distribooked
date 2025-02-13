package it.unipi.distribooked.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OverdueLoanDTO {
    @Schema(description = "The ID of the user who has the overdue loan.",
            example = "63f2a1b7e4b0e74b5c9a5678")
    private String userId;

    @Schema(description = "The ID of the overdue book.",
            example = "63f2a1b7e4b0e74b5c9a9876")
    private String bookId;

    @Schema(description = "The ID of the library where the loan was made.",
            example = "63f2a1b7e4b0e74b5c9a4567")
    private String libraryId;

    @Schema(description = "The timestamp when the book was due.",
            example = "2025-01-23T15:30:00")
    private LocalDateTime dueDate;
}
