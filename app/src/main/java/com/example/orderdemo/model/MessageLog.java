package com.example.orderdemo.model;

import jakarta.persistence.*;

import java.sql.Timestamp;

/**
 * 用于实现 消息幂等性处理 的实体类，它在分布式消息系统中起到防止消息重复处理的关键作用。
 */
@Entity
@Table(name = "message_log", indexes = {
        @Index(name = "uk_dedup_key", columnList = "dedupKey", unique = true)
})
public class MessageLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 128)
    private String dedupKey;

    @Column(length = 128)
    private String messageId;

    @Column(nullable = false, length = 16)
    private String status = "Success";

    @Column(nullable = false, insertable = false, updatable = false)
    private java.sql.Timestamp processedAt;

    public String getStatus() {
        return status;
    }

    public Timestamp getProcessedAt() {
        return processedAt;
    }

    public String getMessageId() {
        return messageId;
    }

    public String getDedupKey() {
        return dedupKey;
    }

    public Long getId() {
        return id;
    }

    public void setDedupKey(String dedupKey) {
        this.dedupKey = dedupKey;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
