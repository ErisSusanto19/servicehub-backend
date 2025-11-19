package com.eris.servicehub.repositories;

import com.eris.servicehub.entities.ServiceImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ServiceImageRepository extends JpaRepository<ServiceImage, UUID> {
}