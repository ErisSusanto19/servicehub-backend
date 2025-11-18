package com.eris.servicehub.security;

import com.eris.servicehub.entities.Order;
import com.eris.servicehub.repositories.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component("orderSecurity")
public class OrderSecurity {

    @Autowired
    private OrderRepository orderRepository;

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
}