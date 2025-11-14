package com.eris.servicehub.services.implementation;

import com.eris.servicehub.dtos.profile.UpdateProfileRequest;
import com.eris.servicehub.dtos.profile.UserProfileResponse;
import com.eris.servicehub.entities.Profile;
import com.eris.servicehub.entities.User;
import com.eris.servicehub.exceptions.ResourceNotFoundException;
import com.eris.servicehub.repositories.UserRepository;
import com.eris.servicehub.services.ProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProfileServiceImpl implements ProfileService {

    @Autowired
    private UserRepository userRepository;

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
}