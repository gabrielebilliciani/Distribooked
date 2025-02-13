package it.unipi.distribooked.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserActiveBookDTO {
    private String bookId;
    private String libraryId;
    private String libraryName;
    private String title;
    private String status;        // "RESERVED" o "LOANED"
    private Long deadlineDate;
}
