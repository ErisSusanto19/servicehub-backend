package com.eris.servicehub.security;

import com.eris.servicehub.entities.Order;
import com.eris.servicehub.entities.OrderItem;
import com.eris.servicehub.repositories.OrderItemRepository;
import com.eris.servicehub.repositories.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component("orderSecurity")
public class OrderSecurity {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    public boolean isProviderForOrder(Authentication authentication, UUID orderId) {
        String currentUsername = authentication.getName();

        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null) {
            return false;
        }

        return order.getOrderItems().stream()
                .anyMatch(item -> item.getService().getProvider().getEmail().equalsIgnoreCase(currentUsername));
    }

    public boolean isCustomerForOrder(Authentication authentication, UUID orderId) {
        String currentUsername = authentication.getName();

        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null) {
            return false;
        }

        return order.getCustomer().getEmail().equalsIgnoreCase(currentUsername);
    }

    public boolean isCustomerForOrderItem(Authentication authentication, UUID orderItemId) {
        String currentUsername = authentication.getName();
        OrderItem orderItem = orderItemRepository.findById(orderItemId).orElse(null);
        if (orderItem == null) {
            return false;
        }
        return orderItem.getOrder().getCustomer().getEmail().equalsIgnoreCase(currentUsername);
    }
}