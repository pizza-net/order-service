package com.pizzanet.orderservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateDeliveryRequest {
    private Long orderId;
    private Long customerId;
    private String deliveryAddress;
    private String customerPhone;
    private String notes;
    private Double latitude;
    private Double longitude;
}
