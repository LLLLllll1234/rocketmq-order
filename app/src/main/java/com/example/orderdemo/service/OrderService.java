package com.example.orderdemo.service;

import com.example.orderdemo.repository.OrderRepository;
import org.springframework.stereotype.Service;

@Service
public class OrderService {
    private final OrderRepository orderRepository;
    private final OrderEventPublisher publisher;


}
