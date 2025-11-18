package com.eris.servicehub.repositories;

import com.eris.servicehub.entities.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {
    List<Order> findByCustomerId(UUID customerId);
    @Query("SELECT DISTINCT o FROM Order o JOIN o.orderItems oi JOIN oi.service s WHERE s.provider.id = :providerId ORDER BY o.createdAt DESC")
    List<Order> findOrdersByProviderId(@Param("providerId") UUID providerId);
}