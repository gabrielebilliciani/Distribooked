package it.unipi.distribooked.service;

import it.unipi.distribooked.dto.LoanDTO;
import it.unipi.distribooked.dto.OverdueLoanDTO;
import it.unipi.distribooked.dto.ReservationDTO;
import it.unipi.distribooked.repository.redis.LoanRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LoanManagementService {

    @Autowired
    private LoanRepository loanRepository;

    /**
     * Get all loans for a specific library.
     *
     * @param libraryId The ID of the library.
     * @return A list of book IDs currently on loan.
     */
    public List<LoanDTO> getAllLoans(String libraryId) {
        return loanRepository.getAllLoans(libraryId);
    }

    /**
     * Get all reservations for a specific library.
     *
     * @param libraryId The ID of the library.
     * @return A list of book IDs currently reserved.
     */
    public List<ReservationDTO> getAllReservations(String libraryId) {
        return loanRepository.getAllReservations(libraryId);
    }

    /**
     * Retrieves all overdue loans for a specific library.
     *
     * @param libraryId The ID of the library.
     * @return A list of overdue loans (book IDs and user IDs).
     */
    public List<OverdueLoanDTO> getOverdueLoans(String libraryId) {
        return loanRepository.getOverdueLoans(libraryId);
    }

    /**
     * Converts a reservation into a loan for a specific user and book in a library.
     *
     * @param libraryId The ID of the library.
     * @param userId    The ID of the user.
     * @param bookId    The ID of the book.
     * @throws IllegalStateException if the reservation does not exist or cannot be converted.
     */
    public void markAsLoan(String libraryId, String userId, String bookId) {
        boolean success = loanRepository.markAsLoan(libraryId, userId, bookId);
        if (!success) {
            throw new IllegalStateException("Reservation not found or could not be marked as a loan.");
        }
    }

    /**
     * Completes a loan for a specific book and user in a library.
     *
     * @param libraryId The ID of the library.
     * @param userId    The ID of the user.
     * @param bookId    The ID of the book.
     * @throws IllegalStateException if the loan does not exist or cannot be completed.
     */
    public void completeLoan(String libraryId, String userId, String bookId) {
        boolean success = loanRepository.completeLoan(libraryId, userId, bookId);
        if (!success) {
            throw new IllegalStateException("Loan not found or could not be completed.");
        }
    }
}

