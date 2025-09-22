package com.example.orderdemo.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "orders", indexes = {
        @Index(name="uk_order_id", columnList="orderId", unique = true)
})
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String orderId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private OrderStatus status;

    @Column(nullable = false, updatable = false, insertable = false)
    private java.sql.Timestamp createdAt;

    @Column(nullable = false, insertable = false)
    private java.sql.Timestamp updatedAt;

    public Long getId() { return id; }
    public String getOrderId() { return orderId; }
    public BigDecimal getAmount() { return amount; }
    public OrderStatus getStatus() { return status; }
    public java.sql.Timestamp getCreatedAt() { return createdAt; }
    public java.sql.Timestamp getUpdatedAt() { return updatedAt; }

    public void setOrderId(String orderId) { this.orderId = orderId; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public void setStatus(OrderStatus status) { this.status = status; }

}
