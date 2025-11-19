package com.eris.servicehub.services.service.implementation;

import com.eris.servicehub.dtos.service.ServiceRequest;
import com.eris.servicehub.dtos.service.ServiceResponse;
import com.eris.servicehub.entities.Category;
import com.eris.servicehub.entities.Service;
import com.eris.servicehub.entities.ServiceImage;
import com.eris.servicehub.entities.User;
import com.eris.servicehub.exceptions.ResourceNotFoundException;
import com.eris.servicehub.repositories.*;
import com.eris.servicehub.repositories.specifications.ServiceSpecification;
import com.eris.servicehub.services.service.ServiceService;
import com.eris.servicehub.services.storage.StorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class ServiceServiceImpl implements ServiceService {

    @Autowired private ServiceRepository serviceRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private ReviewRepository reviewRepository;
    @Autowired private StorageService storageService;
    @Autowired private ServiceImageRepository serviceImageRepository;

    private User getCurrentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String username;
        if (principal instanceof UserDetails) {
            username = ((UserDetails) principal).getUsername();
        } else {
            username = principal.toString();
        }
        return userRepository.findByEmail(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + username));
    }

    private ServiceResponse mapToResponse(Service service) {
        ServiceResponse.ProviderSummary providerSummary = ServiceResponse.ProviderSummary.builder()
                .id(service.getProvider().getId())
                .name(service.getProvider().getName())
                .build();
        ServiceResponse.CategorySummary categorySummary = ServiceResponse.CategorySummary.builder()
                .id(service.getCategory().getId())
                .name(service.getCategory().getName())
                .build();

        Double averageRating = reviewRepository.findAverageRatingByServiceId(service.getId());

        List<ServiceResponse.ImageSummary> imageSummaries = service.getImages().stream()
                .map(image -> ServiceResponse.ImageSummary.builder()
                        .id(image.getId())
                        .imageUrl(image.getImageUrl())
                        .build())
                .toList();

        return ServiceResponse.builder()
                .id(service.getId())
                .name(service.getName())
                .description(service.getDescription())
                .price(service.getPrice())
                .averageRating(averageRating != null ? averageRating : 0.0)
                .images(imageSummaries)
                .provider(providerSummary)
                .category(categorySummary)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ServiceResponse> getAllServices(Pageable pageable, UUID categoryId, String searchQuery) {
        Specification<Service> spec = Specification.anyOf();

        if (categoryId != null) {
            spec = spec.and(ServiceSpecification.byCategoryId(categoryId));
        }

        if (StringUtils.hasText(searchQuery)) {
            spec = spec.and(ServiceSpecification.hasText(searchQuery));
        }

        Page<Service> servicePage = serviceRepository.findAll(spec, pageable);

        return servicePage.map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public ServiceResponse getServiceById(UUID serviceId) {
        Service service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Service not found with id: " + serviceId));
        return mapToResponse(service);
    }

    @Override
    @Transactional
    public ServiceResponse createService(ServiceRequest request) {
        User provider = getCurrentUser();
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + request.getCategoryId()));

        Service service = Service.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .provider(provider)
                .category(category)
                .build();

        Service savedService = serviceRepository.save(service);
        return mapToResponse(savedService);
    }

    @Override
    @Transactional
    public ServiceResponse updateService(UUID serviceId, ServiceRequest request) {
        Service service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Service not found with id: " + serviceId));
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + request.getCategoryId()));

        service.setName(request.getName());
        service.setDescription(request.getDescription());
        service.setPrice(request.getPrice());
        service.setCategory(category);

        Service updatedService = serviceRepository.save(service);
        return mapToResponse(updatedService);
    }

    @Override
    @Transactional
    public void deleteService(UUID serviceId) {
        if (!serviceRepository.existsById(serviceId)) {
            throw new ResourceNotFoundException("Service not found with id: " + serviceId);
        }
        serviceRepository.deleteById(serviceId);
    }

    @Override
    @Transactional
    public ServiceResponse addImageToService(UUID serviceId, MultipartFile file) {
        Service service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Service not found with id: " + serviceId));

        String imageUrl = storageService.uploadFile(file, "servicehub/services");

        ServiceImage serviceImage = ServiceImage.builder()
                .imageUrl(imageUrl)
                .service(service)
                .build();

        service.getImages().add(serviceImage);

        Service updatedService = serviceRepository.save(service);
        return mapToResponse(updatedService);
    }

    @Override
    @Transactional
    public void deleteImageFromService(UUID serviceId, UUID imageId) {
        ServiceImage serviceImage = serviceImageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("Image not found with id: " + imageId));

        if (!serviceImage.getService().getId().equals(serviceId)) {
            throw new org.springframework.security.access.AccessDeniedException("Image does not belong to the specified service.");
        }

        storageService.deleteFile(serviceImage.getImageUrl());

        Service service = serviceImage.getService();
        service.getImages().remove(serviceImage);
        serviceRepository.save(service);
    }
}