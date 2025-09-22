package com.example.orderdemo.repository;

import com.example.orderdemo.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long>{
    Optional<Order> findByOrderId(String orderId);
    boolean existsByOrderId(String orderId);
}
