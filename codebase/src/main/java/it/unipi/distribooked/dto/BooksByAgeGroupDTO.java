package it.unipi.distribooked.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BooksByAgeGroupDTO {
    private String ageGroup;
    private int totalReadings;
    private List<MostReadBookDTO> mostReadBooks;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class MostReadBookDTO {
        private String bookId;
        private String bookTitle;
        private int readCount;
    }
}
