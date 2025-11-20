package com.eris.servicehub.services.order.implementation;

import com.eris.servicehub.dtos.order.OrderRequest;
import com.eris.servicehub.dtos.order.OrderResponse;
import com.eris.servicehub.dtos.order.UpdateOrderStatusRequest;
import com.eris.servicehub.entities.Order;
import com.eris.servicehub.entities.OrderItem;
import com.eris.servicehub.entities.Service;
import com.eris.servicehub.entities.User;
import com.eris.servicehub.enums.OrderStatus;
import com.eris.servicehub.enums.PaymentStatus;
import com.eris.servicehub.exceptions.ResourceNotFoundException;
import com.eris.servicehub.repositories.OrderRepository;
import com.eris.servicehub.repositories.ServiceRepository;
import com.eris.servicehub.repositories.UserRepository;
import com.eris.servicehub.services.notification.NotificationService;
import com.eris.servicehub.services.order.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@org.springframework.stereotype.Service
public class OrderServiceImpl implements OrderService {

    @Autowired private OrderRepository orderRepository;
    @Autowired private ServiceRepository serviceRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private NotificationService notificationService;

    @Value("${platform.fee.percentage}")
    private BigDecimal platformFeePercentage;


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

        Set<User> providers = services.stream()
                .map(Service::getProvider)
                .collect(Collectors.toSet());

        for (User provider : providers) {
            String message = String.format("You have a new order #%s from %s.", savedOrder.getId().toString().substring(0, 8), customer.getName());
            String link = "/provider/orders/" + savedOrder.getId();
            notificationService.createNotification(provider, message, link);
        }

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
                .updatedAt(order.getUpdatedAt())
                .paymentStatus(order.getPaymentStatus())
                .platformFee(order.getPlatformFee())
                .netPayout(order.getNetPayout())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersForProvider() {
        User provider = getCurrentUser();
        List<Order> orders = orderRepository.findOrdersByProviderId(provider.getId());
        return orders.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public OrderResponse updateOrderStatus(UUID orderId, UpdateOrderStatusRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        OrderStatus currentStatus = order.getStatus();
        OrderStatus newStatus = request.status();

        switch (currentStatus) {
            case PENDING:
                if (newStatus == OrderStatus.ACCEPTED && order.getPaymentStatus() != PaymentStatus.PAID) {
                    throw new IllegalStateException("Order must be paid before it can be accepted.");
                }
                if (newStatus != OrderStatus.ACCEPTED && newStatus != OrderStatus.REJECTED) {
                    throw new IllegalArgumentException("From PENDING, provider can only move to ACCEPTED or REJECTED.");
                }
                break;
            case ACCEPTED:
                if (newStatus != OrderStatus.IN_PROGRESS) {
                    throw new IllegalArgumentException("From ACCEPTED, provider can only move to IN_PROGRESS.");
                }
                break;
            case IN_PROGRESS:
                if (newStatus != OrderStatus.COMPLETED) {
                    throw new IllegalArgumentException("From IN_PROGRESS, provider can only move to COMPLETED.");
                }
                calculateFinancials(order);
                break;
            default:
                throw new IllegalStateException("Order status cannot be changed from its current state: " + currentStatus);
        }

        order.setStatus(newStatus);
        Order updatedOrder = orderRepository.save(order);

        String message = String.format("Your order status #%s has been changed to %s.", updatedOrder.getId().toString().substring(0, 8), newStatus);
        String link = "/customer/orders/" + updatedOrder.getId();
        notificationService.createNotification(updatedOrder.getCustomer(), message, link);

        return mapToResponse(updatedOrder);
    }

    private void calculateFinancials(Order order) {
        BigDecimal total = order.getTotalPrice();
        BigDecimal fee = total.multiply(platformFeePercentage).setScale(2, RoundingMode.HALF_UP);
        BigDecimal payout = total.subtract(fee);

        order.setPlatformFee(fee);
        order.setNetPayout(payout);
    }

    @Override
    @Transactional
    public OrderResponse cancelOrder(UUID orderId) {
        User customer = getCurrentUser();
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        if (!order.getCustomer().getId().equals(customer.getId())) {
            throw new org.springframework.security.access.AccessDeniedException("You are not authorized to cancel this order.");
        }

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalStateException("Only orders with PENDING status can be cancelled.");
        }

        order.setStatus(OrderStatus.CANCELLED);
        Order updatedOrder = orderRepository.save(order);

        Set<User> providers = updatedOrder.getOrderItems().stream()
                .map(item -> item.getService().getProvider())
                .collect(Collectors.toSet());

        for (User provider : providers) {
            String message = String.format("Order #%s has been canceled by the customer.", updatedOrder.getId().toString().substring(0, 8));
            String link = "/provider/orders/" + updatedOrder.getId();
            notificationService.createNotification(provider, message, link);
        }

        return mapToResponse(updatedOrder);
    }

    @Override
    @Transactional
    public OrderResponse confirmPayment(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            throw new IllegalStateException("Order has already been paid.");
        }
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new IllegalStateException("Cannot process payment for a cancelled order.");
        }

        order.setPaymentStatus(PaymentStatus.PAID);
        Order updatedOrder = orderRepository.save(order);

        return mapToResponse(updatedOrder);
    }
}