package com.eris.servicehub.services;

import com.eris.servicehub.dtos.profile.UpdateProfileRequest;
import com.eris.servicehub.dtos.profile.UserProfileResponse;

public interface ProfileService {
    UserProfileResponse getMyProfile();
    UserProfileResponse updateMyProfile(UpdateProfileRequest request);
}