package com.eris.servicehub.services.profile.implementation;

import com.eris.servicehub.dtos.profile.ProviderDashboardResponse;
import com.eris.servicehub.dtos.profile.ProviderWalletResponse;
import com.eris.servicehub.dtos.profile.UpdateProfileRequest;
import com.eris.servicehub.dtos.profile.UserProfileResponse;
import com.eris.servicehub.entities.Order;
import com.eris.servicehub.entities.Profile;
import com.eris.servicehub.entities.Role;
import com.eris.servicehub.entities.User;
import com.eris.servicehub.enums.OrderStatus;
import com.eris.servicehub.enums.PaymentStatus;
import com.eris.servicehub.exceptions.ResourceNotFoundException;
import com.eris.servicehub.repositories.OrderRepository;
import com.eris.servicehub.repositories.ReviewRepository;
import com.eris.servicehub.repositories.RoleRepository;
import com.eris.servicehub.repositories.UserRepository;
import com.eris.servicehub.services.profile.ProfileService;
import com.eris.servicehub.services.storage.StorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ProfileServiceImpl implements ProfileService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private StorageService storageService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ReviewRepository reviewRepository;

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

    private UserProfileResponse mapToUserProfileResponse(User user) {
        Profile profile = user.getProfile();
        return UserProfileResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .image(profile != null ? profile.getImage() : null)
                .phone(profile != null ? profile.getPhone() : null)
                .address(profile != null ? profile.getAddress() : null)
                .dateOfBirth(profile != null ? profile.getDateOfBirth() : null)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getMyProfile() {
        User currentUser = getCurrentUser();
        return mapToUserProfileResponse(currentUser);
    }

    @Override
    @Transactional
    public UserProfileResponse updateMyProfile(UpdateProfileRequest request) {
        User currentUser = getCurrentUser();

        currentUser.setName(request.getName());

        Profile profile = currentUser.getProfile();
        if (profile == null) {
            profile = new Profile();
            profile.setUser(currentUser);
            currentUser.setProfile(profile);
        }

        profile.setPhone(request.getPhone());
        profile.setAddress(request.getAddress());
        profile.setDateOfBirth(request.getDateOfBirth());

        User updatedUser = userRepository.save(currentUser);

        return mapToUserProfileResponse(updatedUser);
    }

    @Override
    @Transactional
    public UserProfileResponse becomeProvider() {
        User currentUser = getCurrentUser();

        Role providerRole = roleRepository.findByName("PROVIDER")
                .orElseThrow(() -> new RuntimeException("Fatal: PROVIDER role not found in database."));

        currentUser.getRoles().add(providerRole);

        userRepository.save(currentUser);

        return mapToUserProfileResponse(currentUser);
    }

    @Override
    @Transactional
    public UserProfileResponse updateProfileImage(MultipartFile file){
        User currentUser = getCurrentUser();
        Profile profile = currentUser.getProfile();

        if (profile == null) {
            profile = new Profile();
            profile.setUser(currentUser);
            currentUser.setProfile(profile);
        }

        storageService.deleteFile(profile.getImage());

        String fileUrl = storageService.uploadFile(file, "servicehub/avatars");

        profile.setImage(fileUrl);
        User updatedUser = userRepository.save(currentUser);
        return mapToUserProfileResponse(updatedUser);
    }

    @Override
    @Transactional(readOnly = true)
    public ProviderWalletResponse getProviderWallet() {
        User provider = getCurrentUser();

        List<Order> completedOrders = orderRepository.findCompletedOrdersByProviderId(provider.getId());

        BigDecimal totalGrossRevenue = BigDecimal.ZERO;
        BigDecimal totalNetRevenue = BigDecimal.ZERO;
        BigDecimal pendingPayout = BigDecimal.ZERO;

        for (Order order : completedOrders) {
            totalGrossRevenue = totalGrossRevenue.add(order.getTotalPrice());
            if (order.getPaymentStatus() == PaymentStatus.PAID) {
                totalNetRevenue = totalNetRevenue.add(order.getNetPayout());
            } else {
                pendingPayout = pendingPayout.add(order.getNetPayout());
            }
        }

        List<ProviderWalletResponse.TransactionSummary> transactions = completedOrders.stream()
                .map(order -> ProviderWalletResponse.TransactionSummary.builder()
                        .orderId(order.getId())
                        .completedAt(order.getUpdatedAt())
                        .grossAmount(order.getTotalPrice())
                        .platformFee(order.getPlatformFee())
                        .netAmount(order.getNetPayout())
                        .paymentStatus(order.getPaymentStatus())
                        .build())
                .collect(Collectors.toList());

        return ProviderWalletResponse.builder()
                .totalGrossRevenue(totalGrossRevenue)
                .totalNetRevenue(totalNetRevenue)
                .pendingPayout(pendingPayout)
                .recentTransactions(transactions)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ProviderDashboardResponse getProviderDashboard() {
        User provider = getCurrentUser();
        UUID providerId = provider.getId();

        BigDecimal totalGrossRevenue = orderRepository.findTotalGrossRevenueByProviderId(providerId);
        BigDecimal totalNetRevenue = orderRepository.findTotalNetRevenueByProviderId(providerId);

        BigDecimal pendingPayout = BigDecimal.ZERO;

        long activeOrdersCount = orderRepository.countOrdersByProviderIdAndStatus(providerId, OrderStatus.IN_PROGRESS);
        long completedOrdersCount = orderRepository.countOrdersByProviderIdAndStatus(providerId, OrderStatus.COMPLETED);

        Double averageRating = reviewRepository.findAverageRatingByProviderId(providerId);

        return ProviderDashboardResponse.builder()
                .totalGrossRevenue(totalGrossRevenue)
                .totalNetRevenue(totalNetRevenue)
                .pendingPayout(pendingPayout)
                .activeOrdersCount(activeOrdersCount)
                .completedOrdersCount(completedOrdersCount)
                .averageRating(averageRating != null ? averageRating : 0.0)
                .build();
    }
}