package com.example.orderdemo.controller;

import com.example.orderdemo.service.OrderService;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public Map<String, Object> create (@RequestBody Map<String, Object> body) throws Exception {
        BigDecimal amount = new BigDecimal(String.valueOf(body.getOrDefault("amount", "0")));
        String id = orderService.create(amount);
        return Map.of("orderId", id, "status", "CREATED");
    }

    @PostMapping("/{orderId}/pay")
    public Map<String, Object> pay (@PathVariable String orderId) throws Exception {
        orderService.pay(orderId);
        return Map.of("orderId", orderId, "event", "PAID(queued");
    }

    @PostMapping("/{orderId}/ship")
    public Map<String, Object> ship (@PathVariable String orderId) throws Exception {
        orderService.ship(orderId);
        return Map.of("orderId", orderId, "event", "SHIPPED(queued");
    }
}
