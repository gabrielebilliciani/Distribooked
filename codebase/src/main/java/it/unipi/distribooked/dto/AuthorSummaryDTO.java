package it.unipi.distribooked.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Summary DTO for author information.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthorSummaryDTO {
    private String id;
    private String fullName;
    private String yearOfBirth;
    private String yearOfDeath;
    private String avatarUrl;
    private String about;
}

