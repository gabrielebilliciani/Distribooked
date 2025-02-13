package it.unipi.distribooked.service.notification;

import org.springframework.stereotype.Component;

/**
* This is not going to be implemented in this project.
* It is just a placeholder for future implementations.
*/
public interface NotificationService {
    void sendLoanOverdueNotification(String userId, String bookId, String libraryId);
    void sendExpiredReservationNotification(String bookId, String libraryId);
}