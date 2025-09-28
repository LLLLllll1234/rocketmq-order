package com.example.orderdemo.repository;

import com.example.orderdemo.model.MessageLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageLogRepository extends JpaRepository<MessageLog, Long> {
    Optional<MessageLog> findByDedupKey(String dedupKey);
}
