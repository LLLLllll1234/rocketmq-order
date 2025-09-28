package com.example.orderdemo.service;

import com.example.orderdemo.model.MessageLog;
import com.example.orderdemo.repository.MessageLogRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class IdempotentService {
    private final MessageLogRepository repo;

    public IdempotentService(MessageLogRepository repo) {
        this.repo = repo;
    }

    /**
     * Execute the action at most once per dedupKey.
     * If a record with dedupKey already exists, the action is skipped.
     */
    @Transactional
    public boolean processOnce(String dedupKey, String messageId, Runnable action) {
        return repo.findByDedupKey(dedupKey)
                .map(x -> false)
                .orElseGet(() -> {
                    // no record -> run and insert a success record
                    action.run();
                    MessageLog log = new MessageLog();
                    log.setDedupKey(dedupKey);
                    log.setMessageId(messageId);
                    log.setStatus("SUCCESS");
                    repo.save(log);
                    return true;
                });
    }
}
