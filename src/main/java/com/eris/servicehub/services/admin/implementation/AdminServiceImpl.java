package com.eris.servicehub.services.admin.implementation;

import com.eris.servicehub.dtos.admin.UserSummaryResponse;
import com.eris.servicehub.dtos.service.ServiceResponse;
import com.eris.servicehub.entities.Role;
import com.eris.servicehub.entities.User;
import com.eris.servicehub.exceptions.ResourceNotFoundException;
import com.eris.servicehub.repositories.ReviewRepository;
import com.eris.servicehub.repositories.ServiceRepository;
import com.eris.servicehub.repositories.UserRepository;
import com.eris.servicehub.services.admin.AdminService;
import com.eris.servicehub.services.storage.StorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AdminServiceImpl implements AdminService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ServiceRepository serviceRepository;

    @Autowired
    private StorageService storageService;

    @Autowired
    private ReviewRepository reviewRepository;


    @Override
    @Transactional(readOnly = true)
    public Page<UserSummaryResponse> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(this::mapToUserSummaryResponse);
    }

    @Override
    @Transactional
    public UserSummaryResponse setUserEnabledStatus(UUID userId, boolean enabled) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        user.setEnabled(enabled);
        User updatedUser = userRepository.save(user);
        return mapToUserSummaryResponse(updatedUser);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ServiceResponse> getAllServices(Pageable pageable) {
        return serviceRepository.findAllWithDetails(pageable).map(this::mapToServiceResponse);
    }

    @Override
    @Transactional
    public void deleteService(UUID serviceId) {
        com.eris.servicehub.entities.Service service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Service not found with id: " + serviceId));

        service.getImages().forEach(image -> storageService.deleteFile(image.getImageUrl()));

        serviceRepository.delete(service);
    }

    private UserSummaryResponse mapToUserSummaryResponse(User user) {
        return UserSummaryResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .roles(user.getRoles().stream().map(Role::getName).collect(Collectors.toSet()))
                .enabled(user.isEnabled())
                .createdAt(user.getCreatedAt())
                .build();
    }

    private ServiceResponse mapToServiceResponse(com.eris.servicehub.entities.Service service) {
        ServiceResponse.ProviderSummary providerSummary = ServiceResponse.ProviderSummary.builder()
                .id(service.getProvider().getId())
                .name(service.getProvider().getName())
                .build();

        ServiceResponse.CategorySummary categorySummary = ServiceResponse.CategorySummary.builder()
                .id(service.getCategory().getId())
                .name(service.getCategory().getName())
                .build();

        List<ServiceResponse.ImageSummary> imageSummaries = service.getImages().stream()
                .map(image -> ServiceResponse.ImageSummary.builder()
                        .id(image.getId())
                        .imageUrl(image.getImageUrl())
                        .build())
                .collect(Collectors.toList());

        Double averageRating = reviewRepository.findAverageRatingByServiceId(service.getId());

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
}