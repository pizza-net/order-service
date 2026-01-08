package com.pizzanet.orderservice.dto;

import com.pizzanet.orderservice.model.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {
    private Long id;
    private Long customerId;
    private String customerName;
    private String deliveryAddress;
    private String customerPhone;
    private String notes;
    private OrderStatus status;
    private BigDecimal totalPrice;
    private List<OrderItemResponse> items;
    private Long deliveryId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
