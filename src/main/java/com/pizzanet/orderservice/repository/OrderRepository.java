package com.pizzanet.orderservice.repository;

import com.pizzanet.orderservice.model.Order;
import com.pizzanet.orderservice.model.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByCustomerId(Long customerId);
    List<Order> findByStatus(OrderStatus status);
    List<Order> findByCustomerIdOrderByCreatedAtDesc(Long customerId);
}
