package com.eris.servicehub.services.kafka;

import com.eris.servicehub.entities.Order;
import com.eris.servicehub.entities.User;
import com.eris.servicehub.exceptions.ResourceNotFoundException;
import com.eris.servicehub.repositories.OrderRepository;
import com.eris.servicehub.services.notification.NotificationService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class NotificationConsumer {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private OrderRepository orderRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @KafkaListener(topics = "order_created", groupId = "servicehub-group")
    public void handleOrderCreated(String message) {
        System.out.println("Received order_created event: " + message);

        try {
            JsonNode jsonNode = objectMapper.readTree(message);
            UUID orderId = UUID.fromString(jsonNode.get("orderId").asText());

            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new ResourceNotFoundException("Order not found for notification: " + orderId));

            User customer = order.getCustomer();

            Set<User> providers = order.getOrderItems().stream()
                    .map(item -> item.getService().getProvider())
                    .collect(Collectors.toSet());

            for (User provider : providers) {
                String notificationMessage = String.format("You have a new order #%s from %s.",
                        order.getId().toString().substring(0, 8), customer.getName());
                String link = "/provider/orders/" + order.getId();
                notificationService.createNotification(provider, notificationMessage, link);
                System.out.println("Created notification for provider " + provider.getId());
            }

        } catch (JsonProcessingException e) {
            System.err.println("Failed to parse order_created event: " + message);

        }
    }
}