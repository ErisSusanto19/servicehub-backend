package com.eris.servicehub.repositories;

import com.eris.servicehub.entities.Order;
import com.eris.servicehub.enums.OrderStatus;
import com.eris.servicehub.enums.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {
    Page<Order> findByCustomerId(UUID customerId, Pageable pageable);

    @Query(
            value = "SELECT DISTINCT o FROM Order o JOIN o.orderItems oi JOIN oi.service s WHERE s.provider.id = :providerId ORDER BY o.createdAt DESC",
            countQuery = "SELECT count(DISTINCT o) FROM Order o JOIN o.orderItems oi JOIN oi.service s WHERE s.provider.id = :providerId"
    )
    Page<Order> findOrdersByProviderId(@Param("providerId") UUID providerId, Pageable pageable);

    @Query("SELECT o FROM Order o JOIN o.orderItems oi JOIN oi.service s WHERE s.provider.id = :providerId AND o.status = 'COMPLETED' ORDER BY o.updatedAt DESC")
    List<Order> findCompletedOrdersByProviderId(@Param("providerId") UUID providerId);

    @Query("SELECT o FROM Order o JOIN o.orderItems oi JOIN oi.service s WHERE s.provider.id = :providerId AND o.status = :status AND o.paymentStatus = :paymentStatus AND o.payoutItem IS NULL")
    List<Order> findPayableOrdersByProviderId(
            @Param("providerId") UUID providerId,
            @Param("status") OrderStatus status,
            @Param("paymentStatus") PaymentStatus paymentStatus
    );

    @Override
    @EntityGraph(attributePaths = {"orderItems", "orderItems.service"})
    Optional<Order> findById(UUID id);

    @Query("SELECT COALESCE(SUM(o.totalPrice), 0) FROM Order o JOIN o.orderItems oi JOIN oi.service s WHERE s.provider.id = :providerId AND o.status = 'COMPLETED'")
    BigDecimal findTotalGrossRevenueByProviderId(UUID providerId);

    @Query("SELECT COALESCE(SUM(o.netPayout), 0) FROM Order o JOIN o.orderItems oi JOIN oi.service s WHERE s.provider.id = :providerId AND o.status = 'COMPLETED'")
    BigDecimal findTotalNetRevenueByProviderId(UUID providerId);

    @Query("SELECT COUNT(DISTINCT o.id) FROM Order o JOIN o.orderItems oi JOIN oi.service s WHERE s.provider.id = :providerId AND o.status = :status")
    long countOrdersByProviderIdAndStatus(UUID providerId, OrderStatus status);
}