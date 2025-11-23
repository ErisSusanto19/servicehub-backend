package com.eris.servicehub.services.order;

import com.eris.servicehub.dtos.order.OrderRequest;
import com.eris.servicehub.dtos.order.OrderResponse;
import com.eris.servicehub.dtos.order.UpdateOrderStatusRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface OrderService {
    OrderResponse createOrder(OrderRequest request);
    Page<OrderResponse> getMyOrders(Pageable pageable);
    Page<OrderResponse> getOrdersForProvider(Pageable pageable);
    OrderResponse updateOrderStatus(UUID orderId, UpdateOrderStatusRequest request);
    OrderResponse cancelOrder(UUID orderId);
    OrderResponse confirmPayment(UUID orderId);
    Map<String, String> getOrderPaymentStatus(UUID orderId);
}