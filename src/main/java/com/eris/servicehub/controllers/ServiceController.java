package com.eris.servicehub.controllers;

import com.eris.servicehub.dtos.common.ApiResponse;
import com.eris.servicehub.dtos.common.PagedResponse;
import com.eris.servicehub.dtos.service.ServiceRequest;
import com.eris.servicehub.dtos.service.ServiceResponse;
import com.eris.servicehub.services.service.ServiceService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/services")
public class ServiceController {

    @Autowired
    private ServiceService serviceService;

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<ServiceResponse>>> getAllServices(
            Pageable pageable,
            @RequestParam(required = false) UUID categoryId
    ) {
        Page<ServiceResponse> servicePage = serviceService.getAllServices(pageable, categoryId);
        PagedResponse<ServiceResponse> pagedData = PagedResponse.<ServiceResponse>builder()
                .content(servicePage.getContent())
                .pageNumber(servicePage.getNumber())
                .pageSize(servicePage.getSize())
                .totalElements(servicePage.getTotalElements())
                .totalPages(servicePage.getTotalPages())
                .first(servicePage.isFirst())
                .last(servicePage.isLast())
                .build();
        return ResponseEntity.ok(ApiResponse.success(pagedData, "Services retrieved successfully"));
    }

    @GetMapping("/{serviceId}")
    public ResponseEntity<ApiResponse<ServiceResponse>> getServiceById(@PathVariable UUID serviceId) {
        ServiceResponse data = serviceService.getServiceById(serviceId);
        return ResponseEntity.ok(ApiResponse.success(data, "Service retrieved successfully"));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PROVIDER')")
    public ResponseEntity<ApiResponse<ServiceResponse>> createService(@Valid @RequestBody ServiceRequest request) {
        ServiceResponse data = serviceService.createService(request);
        return new ResponseEntity<>(ApiResponse.success(data, "Service created successfully"), HttpStatus.CREATED);
    }

    @PutMapping("/{serviceId}")
    @PreAuthorize("hasAuthority('PROVIDER') and @serviceSecurity.isOwner(authentication, #serviceId)")
    public ResponseEntity<ApiResponse<ServiceResponse>> updateService(
            @PathVariable UUID serviceId,
            @Valid @RequestBody ServiceRequest request
    ) {
        ServiceResponse data = serviceService.updateService(serviceId, request);
        return ResponseEntity.ok(ApiResponse.success(data, "Service updated successfully"));
    }

    @DeleteMapping("/{serviceId}")
    @PreAuthorize("hasAuthority('PROVIDER') and @serviceSecurity.isOwner(authentication, #serviceId)")
    public ResponseEntity<ApiResponse<Void>> deleteService(@PathVariable UUID serviceId) {
        serviceService.deleteService(serviceId);
        return ResponseEntity.ok(ApiResponse.success(null, "Service deleted successfully"));
    }
}