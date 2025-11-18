package com.eris.servicehub.repositories;

import com.eris.servicehub.entities.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReviewRepository extends JpaRepository<Review, UUID> {

    List<Review> findByServiceId(UUID serviceId);

    boolean existsByOrderItemId(UUID orderItemId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.service.id = :serviceId")
    Double findAverageRatingByServiceId(UUID serviceId);
}