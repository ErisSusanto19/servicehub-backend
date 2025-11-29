package com.eris.servicehub.repositories;

import com.eris.servicehub.entities.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReviewRepository extends JpaRepository<Review, UUID> {

    Page<Review> findByServiceId(UUID serviceId, Pageable pageable);

    boolean existsByOrderItemId(UUID orderItemId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.service.id = :serviceId")
    Double findAverageRatingByServiceId(UUID serviceId);

    @Query("SELECT AVG(r.rating) FROM Review r JOIN r.service s WHERE s.provider.id = :providerId")
    Double findAverageRatingByProviderId(UUID providerId);
}