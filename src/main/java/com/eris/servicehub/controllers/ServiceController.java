package com.eris.servicehub.controllers;

import com.eris.servicehub.dtos.common.ApiResponse;
import com.eris.servicehub.dtos.common.PagedResponse;
import com.eris.servicehub.dtos.review.ReviewResponse;
import com.eris.servicehub.dtos.service.ServiceRequest;
import com.eris.servicehub.dtos.service.ServiceResponse;
import com.eris.servicehub.services.review.ReviewService;
import com.eris.servicehub.services.service.ServiceService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/services")
public class ServiceController {

    @Autowired
    private ServiceService serviceService;

    @Autowired
    private ReviewService reviewService;

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<ServiceResponse>>> getAllServices(
            Pageable pageable,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false, name = "q") String searchQuery
    ) {
        Page<ServiceResponse> servicePage = serviceService.getAllServices(pageable, categoryId, searchQuery);
//        PagedResponse<ServiceResponse> pagedData = PagedResponse.<ServiceResponse>builder()
//                .content(servicePage.getContent())
//                .pageNumber(servicePage.getNumber())
//                .pageSize(servicePage.getSize())
//                .totalElements(servicePage.getTotalElements())
//                .totalPages(servicePage.getTotalPages())
//                .first(servicePage.isFirst())
//                .last(servicePage.isLast())
//                .build();
        PagedResponse<ServiceResponse> pagedData = PagedResponse.from(servicePage);
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

    @GetMapping("/{serviceId}/reviews")
    public ResponseEntity<ApiResponse<List<ReviewResponse>>> getReviewsForService(@PathVariable UUID serviceId) {
        List<ReviewResponse> data = reviewService.getReviewsForService(serviceId);
        return ResponseEntity.ok(ApiResponse.success(data, "Service reviews retrieved successfully"));
    }

    @PostMapping("/{serviceId}/images")
    @PreAuthorize("hasAuthority('PROVIDER') and @serviceSecurity.isOwner(authentication, #serviceId)")
    public ResponseEntity<ApiResponse<ServiceResponse>> addImageToService(
            @PathVariable UUID serviceId,
            @RequestParam("file") MultipartFile file
    ) {
        ServiceResponse data = serviceService.addImageToService(serviceId, file);
        return new ResponseEntity<>(ApiResponse.success(data, "Image added successfully"), HttpStatus.CREATED);
    }

    @DeleteMapping("/{serviceId}/images/{imageId}")
    @PreAuthorize("hasAuthority('PROVIDER') and @serviceSecurity.isOwner(authentication, #serviceId)")
    public ResponseEntity<ApiResponse<Void>> deleteImageFromService(
            @PathVariable UUID serviceId,
            @PathVariable UUID imageId
    ) {
        serviceService.deleteImageFromService(serviceId, imageId);
        return ResponseEntity.ok(ApiResponse.success(null, "Image deleted successfully"));
    }
}