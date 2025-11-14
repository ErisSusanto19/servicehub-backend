package com.eris.servicehub.controllers;

import com.eris.servicehub.dtos.common.ApiResponse;
import com.eris.servicehub.dtos.profile.UpdateProfileRequest;
import com.eris.servicehub.dtos.profile.UserProfileResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getMyProfile() {
        UserProfileResponse dummyData = UserProfileResponse.builder().name("Dummy User").build();
        return ResponseEntity.ok(ApiResponse.success(dummyData, "Profile retrieved successfully"));
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateMyProfile(
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        UserProfileResponse dummyData = UserProfileResponse.builder().name(request.getName()).build();
        return ResponseEntity.ok(ApiResponse.success(dummyData, "Profile updated successfully"));
    }
}