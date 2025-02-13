package it.unipi.distribooked.service.outbox;

import it.unipi.distribooked.model.OutboxTask;
import it.unipi.distribooked.repository.redis.OverdueLoanRepository;
import it.unipi.distribooked.service.notification.NotificationService;
import it.unipi.distribooked.utils.RedisKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component("MARK_LOAN_OVERDUE")
public class MarkLoanOverdueProcessor implements OutboxTaskProcessor {

    private final OverdueLoanRepository overdueLoanRepository;
    private final NotificationService notificationService;

    public MarkLoanOverdueProcessor(
            OverdueLoanRepository overdueLoanRepository,
            @Autowired(required = false) NotificationService notificationService) {
        this.overdueLoanRepository = overdueLoanRepository;
        this.notificationService = notificationService;
    }

    @Override
    public void process(OutboxTask task) {
        String userId = (String) task.getPayload().get("userId");
        String bookId = (String) task.getPayload().get("bookId");
        String libraryId = (String) task.getPayload().get("libraryId");

        boolean success = overdueLoanRepository.markLoanAsOverdue(userId, bookId, libraryId);
        if (!success) {
            throw new IllegalStateException(String.format(
                    "Failed to add overdue loan for user %s book %s in library %s",
                    userId, bookId, libraryId));
        }

        // Simulate notification sending
        if (notificationService != null) {
            notificationService.sendLoanOverdueNotification(userId, bookId, libraryId);
        }
    }
}