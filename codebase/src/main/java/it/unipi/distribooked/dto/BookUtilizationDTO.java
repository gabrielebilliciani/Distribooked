package it.unipi.distribooked.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookUtilizationDTO {
    private String bookId;
    private String title;
    private int totalReadings;
    private int totalCopies;
    private int totalBranches;
    private double avgCopiesPerBranch;
    private double usageRatio;
}