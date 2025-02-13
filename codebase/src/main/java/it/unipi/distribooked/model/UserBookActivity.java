package it.unipi.distribooked.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserBookActivity {
    private Status status;        // "RESERVED" or "LOANED"
    private String title;
    private String libraryName;
    private Long deadlineDate;  // max date for the book to be returned or picked up

    public static enum Status {
        RESERVED,
        LOANED
    }

    public LocalDateTime getDeadlineAsLocalDateTime() {
        return deadlineDate == null
                ? null
                : Instant.ofEpochMilli(deadlineDate).atZone(ZoneId.systemDefault()).toLocalDateTime();
    }
}
