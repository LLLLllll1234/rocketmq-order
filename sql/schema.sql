-- Initializes demo schema and table
CREATE DATABASE IF NOT EXISTS orderdb CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE orderdb;

CREATE TABLE IF NOT EXISTS orders (
                                      id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                      order_id VARCHAR(64) NOT NULL UNIQUE,
    amount DECIMAL(12,2) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Add CANCELLED status through app logic (enum already governs values)

-- Idempotency message log
CREATE TABLE IF NOT EXISTS message_log (
                                           id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                           dedup_key VARCHAR(128) NOT NULL UNIQUE,
    message_id VARCHAR(128) NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'SUCCESS',
    processed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE utf8mb4_unicode_ci;
