package com.eris.servicehub.services.service;

import com.eris.servicehub.dtos.service.ServiceRequest;
import com.eris.servicehub.dtos.service.ServiceResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface ServiceService {
    Page<ServiceResponse> getAllServices(Pageable pageable, UUID categoryId, String searchQuery);
    ServiceResponse getServiceById(UUID serviceId);
    ServiceResponse createService(ServiceRequest request);
    ServiceResponse updateService(UUID serviceId, ServiceRequest request);
    void deleteService(UUID serviceId);
    ServiceResponse addImageToService(UUID serviceId, MultipartFile file);
    void deleteImageFromService(UUID serviceId, UUID imageId);
}