package com.pizzanet.orderservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemResponse {
    private Long id;
    private Long pizzaId;
    private String pizzaName;
    private Integer quantity;
    private BigDecimal pricePerItem;
    private BigDecimal subtotal;
}
