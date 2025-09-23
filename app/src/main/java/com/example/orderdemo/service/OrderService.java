package com.example.orderdemo.service;

import com.example.orderdemo.repository.OrderRepository;
import com.example.orderdemo.rocketmq.OrderEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class OrderService {
    private final OrderRepository orderRepo;
    private final OrderEventPublisher publisher;

    public OrderService(OrderRepository orderRepo, OrderEventPublisher publisher) {
        this.orderRepo = orderRepo;
        this.publisher = publisher;
    }

    /** Create order using RocketMQ TRANSACTION message for consistency. */
    @Transactional
    public String create(BigDecimal amount) {
        String orderId = "order-" + UUID.randomUUID().toString().substring(0, 8);
        // 1) Send half message and begin transaction
        publisher.sendTxnCreated(orderId);
    }

}
