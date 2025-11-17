package com.eris.servicehub.services.order.implementation;

import com.eris.servicehub.dtos.order.OrderRequest;
import com.eris.servicehub.dtos.order.OrderResponse;
import com.eris.servicehub.entities.Order;
import com.eris.servicehub.entities.OrderItem;
import com.eris.servicehub.entities.Service;
import com.eris.servicehub.entities.User;
import com.eris.servicehub.enums.OrderStatus;
import com.eris.servicehub.exceptions.ResourceNotFoundException;
import com.eris.servicehub.repositories.OrderRepository;
import com.eris.servicehub.repositories.ServiceRepository;
import com.eris.servicehub.repositories.UserRepository;
import com.eris.servicehub.services.order.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@org.springframework.stereotype.Service
public class OrderServiceImpl implements OrderService {

    @Autowired private OrderRepository orderRepository;
    @Autowired private ServiceRepository serviceRepository;
    @Autowired private UserRepository userRepository;

    private User getCurrentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String username = (principal instanceof UserDetails) ? ((UserDetails) principal).getUsername() : principal.toString();
        return userRepository.findByEmail(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + username));
    }

    @Override
    @Transactional
    public OrderResponse createOrder(OrderRequest request) {
        User customer = getCurrentUser();

        List<Service> services = serviceRepository.findAllById(request.serviceIds());
        if (services.size() != request.serviceIds().size()) {
            throw new ResourceNotFoundException("One or more services could not be found.");
        }

        BigDecimal totalPrice = services.stream()
                .map(Service::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Order newOrder = Order.builder()
                .customer(customer)
                .totalPrice(totalPrice)
                .status(OrderStatus.PENDING)
                .build();

        List<OrderItem> orderItems = services.stream()
                .map(service -> OrderItem.builder()
                        .order(newOrder)
                        .service(service)
                        .priceAtOrder(service.getPrice())
                        .build())
                .collect(Collectors.toList());

        newOrder.setOrderItems(orderItems);

        Order savedOrder = orderRepository.save(newOrder);

        return mapToResponse(savedOrder);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getMyOrders() {
        User customer = getCurrentUser();
        List<Order> orders = orderRepository.findByCustomerId(customer.getId());
        return orders.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private OrderResponse mapToResponse(Order order) {
        List<OrderResponse.OrderItemResponse> itemResponses = order.getOrderItems().stream()
                .map(item -> {
                    Service service = item.getService();
                    User provider = service.getProvider();

                    OrderResponse.ProviderSummary providerSummary = OrderResponse.ProviderSummary.builder()
                            .id(provider.getId())
                            .name(provider.getName())
                            .build();

                    OrderResponse.ServiceSummary serviceSummary = OrderResponse.ServiceSummary.builder()
                            .id(service.getId())
                            .name(service.getName())
                            .provider(providerSummary)
                            .build();

                    return OrderResponse.OrderItemResponse.builder()
                            .id(item.getId())
                            .service(serviceSummary)
                            .priceAtOrder(item.getPriceAtOrder())
                            .build();
                })
                .collect(Collectors.toList());

        OrderResponse.CustomerSummary customerSummary = OrderResponse.CustomerSummary.builder()
                .id(order.getCustomer().getId())
                .name(order.getCustomer().getName())
                .build();

        return OrderResponse.builder()
                .id(order.getId())
                .customer(customerSummary)
                .orderItems(itemResponses)
                .totalPrice(order.getTotalPrice())
                .status(order.getStatus())
                .createdAt(order.getCreatedAt())
                .build();
    }
}