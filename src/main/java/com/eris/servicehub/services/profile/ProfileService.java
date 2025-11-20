package com.eris.servicehub.services.profile;

import com.eris.servicehub.dtos.profile.ProviderWalletResponse;
import com.eris.servicehub.dtos.profile.UpdateProfileRequest;
import com.eris.servicehub.dtos.profile.UserProfileResponse;
import org.springframework.web.multipart.MultipartFile;

public interface ProfileService {
    UserProfileResponse getMyProfile();
    UserProfileResponse updateMyProfile(UpdateProfileRequest request);
    UserProfileResponse becomeProvider();
    UserProfileResponse updateProfileImage(MultipartFile file);
    ProviderWalletResponse getProviderWallet();
}