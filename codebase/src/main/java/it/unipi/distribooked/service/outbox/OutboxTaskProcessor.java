package it.unipi.distribooked.service.outbox;

import it.unipi.distribooked.exceptions.TaskExecutionException;
import it.unipi.distribooked.model.OutboxTask;

public interface OutboxTaskProcessor {
    void process(OutboxTask task) throws TaskExecutionException;
}
