package com.eris.servicehub.repositories;

import com.eris.servicehub.entities.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ServiceRepository extends JpaRepository<Service, UUID>, JpaSpecificationExecutor<Service> {
    Page<Service> findByCategoryId(UUID categoryId, Pageable pageable);
    @Query(value = "SELECT s FROM Service s JOIN FETCH s.provider JOIN FETCH s.category LEFT JOIN FETCH s.images",
            countQuery = "SELECT COUNT(s) FROM Service s")
    Page<Service> findAllWithDetails(Pageable pageable);
}