package com.eris.servicehub.controllers;

import com.eris.servicehub.dtos.common.ApiResponse;
import com.eris.servicehub.dtos.profile.ProviderWalletResponse;
import com.eris.servicehub.dtos.profile.UpdateProfileRequest;
import com.eris.servicehub.dtos.profile.UserProfileResponse;
import com.eris.servicehub.services.profile.ProfileService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    @Autowired
    private ProfileService profileService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getMyProfile() {
        UserProfileResponse data = profileService.getMyProfile();
        return ResponseEntity.ok(ApiResponse.success(data, "Profile retrieved successfully"));
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateMyProfile(
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        UserProfileResponse data = profileService.updateMyProfile(request);
        return ResponseEntity.ok(ApiResponse.success(data, "Profile updated successfully"));
    }

    @PostMapping("/me/become-provider")
    public ResponseEntity<ApiResponse<UserProfileResponse>> becomeProvider() {
        UserProfileResponse data = profileService.becomeProvider();
        return ResponseEntity.ok(ApiResponse.success(data, "User is now a provider"));
    }

    @PostMapping("/me/image")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateMyProfileImage(
            @RequestParam("file") MultipartFile file
    ) {
        UserProfileResponse data = profileService.updateProfileImage(file);
        return ResponseEntity.ok(ApiResponse.success(data, "Profile image updated successfully"));
    }

    @GetMapping("/me/wallet")
    @PreAuthorize("hasAuthority('PROVIDER')")
    public ResponseEntity<ApiResponse<ProviderWalletResponse>> getMyWallet() {
        ProviderWalletResponse data = profileService.getProviderWallet();
        return ResponseEntity.ok(ApiResponse.success(data, "Provider wallet retrieved successfully"));
    }
}