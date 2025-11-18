package com.eris.servicehub.services.order;

import com.eris.servicehub.dtos.order.OrderRequest;
import com.eris.servicehub.dtos.order.OrderResponse;
import com.eris.servicehub.dtos.order.UpdateOrderStatusRequest;

import java.util.List;
import java.util.UUID;

public interface OrderService {
    OrderResponse createOrder(OrderRequest request);
    List<OrderResponse> getMyOrders();
    List<OrderResponse> getOrdersForProvider();
    OrderResponse updateOrderStatus(UUID orderId, UpdateOrderStatusRequest request);
}