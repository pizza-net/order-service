package com.pizzanet.orderservice.service;

import com.pizzanet.orderservice.dto.*;
import com.pizzanet.orderservice.model.Order;
import com.pizzanet.orderservice.model.OrderItem;
import com.pizzanet.orderservice.model.OrderStatus;
import com.pizzanet.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {
    
    private final OrderRepository orderRepository;
    private final WebClient menuServiceClient;
    private final WebClient deliveryServiceClient;
    private final WebClient authServiceClient;
    private final EventPublisherService eventPublisher;
    
    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        log.info("Creating order for customer: {}", request.getCustomerId());
        
        Order order = Order.builder()
                .customerId(request.getCustomerId())
                .customerName(request.getCustomerName())
                .deliveryAddress(request.getDeliveryAddress())
                .customerPhone(request.getCustomerPhone())
                .notes(request.getNotes())
                .status(OrderStatus.PENDING)
                .totalPrice(BigDecimal.ZERO)
                .build();
        
        BigDecimal totalPrice = BigDecimal.ZERO;
        
        for (OrderItemRequest itemRequest : request.getItems()) {
            PizzaDTO pizza = fetchPizzaDetails(itemRequest.getPizzaId());
            
            if (pizza == null || !pizza.getAvailable()) {
                throw new IllegalArgumentException("Pizza with ID " + itemRequest.getPizzaId() + " is not available");
            }
            
            BigDecimal subtotal = pizza.getPrice().multiply(BigDecimal.valueOf(itemRequest.getQuantity()));
            
            OrderItem orderItem = OrderItem.builder()
                    .pizzaId(pizza.getId())
                    .pizzaName(pizza.getName())
                    .quantity(itemRequest.getQuantity())
                    .pricePerItem(pizza.getPrice())
                    .subtotal(subtotal)
                    .build();
            
            order.addItem(orderItem);
            totalPrice = totalPrice.add(subtotal);
        }
        
        order.setTotalPrice(totalPrice);
        Order savedOrder = orderRepository.save(order);
        
        log.info("Order created with ID: {}", savedOrder.getId());
        
        try {
            Long deliveryId = createDelivery(savedOrder, request);
            savedOrder.setDeliveryId(deliveryId);
            savedOrder.setStatus(OrderStatus.CONFIRMED);
            savedOrder = orderRepository.save(savedOrder);
            log.info("Delivery created with ID: {} for order: {}", deliveryId, savedOrder.getId());
        } catch (Exception e) {
            log.error("Failed to create delivery for order: {}", savedOrder.getId(), e);
        }
        
        return mapToResponse(savedOrder);
    }
    
    private PizzaDTO fetchPizzaDetails(Long pizzaId) {
        try {
            return menuServiceClient.get()
                    .uri("/api/pizza/{id}", pizzaId)
                    .retrieve()
                    .bodyToMono(PizzaDTO.class)
                    .block();
        } catch (Exception e) {
            log.error("Failed to fetch pizza details for ID: {}", pizzaId, e);
            throw new IllegalArgumentException("Failed to fetch pizza details");
        }
    }
    
    private Long createDelivery(Order order, CreateOrderRequest request) {
        CreateDeliveryRequest deliveryRequest = CreateDeliveryRequest.builder()
                .orderId(order.getId())
                .customerId(order.getCustomerId())
                .deliveryAddress(order.getDeliveryAddress())
                .customerPhone(order.getCustomerPhone())
                .notes(order.getNotes())
                .latitude(52.229676)
                .longitude(21.012229)
                .build();
        
        try {
            DeliveryResponse response = deliveryServiceClient.post()
                    .uri("/api/deliveries")
                    .body(Mono.just(deliveryRequest), CreateDeliveryRequest.class)
                    .retrieve()
                    .bodyToMono(DeliveryResponse.class)
                    .block();
            
            return response != null ? response.getId() : null;
        } catch (Exception e) {
            log.error("Failed to create delivery", e);
            throw new RuntimeException("Failed to create delivery");
        }
    }
    
    private String fetchUserEmail(Long userId) {
        try {
            UserDTO user = authServiceClient.get()
                    .uri("/api/auth/users/{id}", userId)
                    .retrieve()
                    .bodyToMono(UserDTO.class)
                    .block();
            
            return user != null ? user.getEmail() : "no-email@pizzanet.com";
        } catch (Exception e) {
            log.error("Failed to fetch user email for ID: {}", userId, e);
            return "no-email@pizzanet.com";
        }
    }
    
    public OrderResponse getOrderById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Order not found with ID: " + id));
        return mapToResponse(order);
    }
    
    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    public List<OrderResponse> getOrdersByCustomerId(Long customerId) {
        return orderRepository.findByCustomerIdOrderByCreatedAtDesc(customerId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    public List<OrderResponse> getOrdersByStatus(OrderStatus status) {
        return orderRepository.findByStatus(status).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    @Transactional
    public OrderResponse updateOrderStatus(Long id, OrderStatus status) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Order not found with ID: " + id));
        
        order.setStatus(status);
        Order updatedOrder = orderRepository.save(order);
        
        log.info("Order {} status updated to: {}", id, status);
        
        // Publikuj event do RabbitMQ
        try {
            String userEmail = fetchUserEmail(updatedOrder.getCustomerId());
            
            OrderStatusChangedEvent event = OrderStatusChangedEvent.builder()
                    .orderId(updatedOrder.getId())
                    .userId(updatedOrder.getCustomerId())
                    .userEmail(userEmail)
                    .orderStatus(status.name())
                    .totalPrice(updatedOrder.getTotalPrice().doubleValue())
                    .timestamp(java.time.LocalDateTime.now())
                    .build();
            
            eventPublisher.publishOrderStatusChanged(event);
        } catch (Exception e) {
            log.error("Failed to publish order status event", e);
            // Nie rzucamy wyjątku - zamówienie zostało zapisane
        }
        
        return mapToResponse(updatedOrder);
    }
    
    @Transactional
    public void deleteOrder(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Order not found with ID: " + id));
        orderRepository.delete(order);
        log.info("Order deleted: {}", id);
    }
    
    private OrderResponse mapToResponse(Order order) {
        List<OrderItemResponse> items = order.getItems().stream()
                .map(item -> OrderItemResponse.builder()
                        .id(item.getId())
                        .pizzaId(item.getPizzaId())
                        .pizzaName(item.getPizzaName())
                        .quantity(item.getQuantity())
                        .pricePerItem(item.getPricePerItem())
                        .subtotal(item.getSubtotal())
                        .build())
                .collect(Collectors.toList());
        
        return OrderResponse.builder()
                .id(order.getId())
                .customerId(order.getCustomerId())
                .customerName(order.getCustomerName())
                .deliveryAddress(order.getDeliveryAddress())
                .customerPhone(order.getCustomerPhone())
                .notes(order.getNotes())
                .status(order.getStatus())
                .totalPrice(order.getTotalPrice())
                .items(items)
                .deliveryId(order.getDeliveryId())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }
}
