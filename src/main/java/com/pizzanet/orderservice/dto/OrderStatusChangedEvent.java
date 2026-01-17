package com.pizzanet.orderservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatusChangedEvent {
    private Long orderId;
    private Long userId;
    private String userEmail;
    private String orderStatus;
    private Double totalPrice;
    private LocalDateTime timestamp;
}
