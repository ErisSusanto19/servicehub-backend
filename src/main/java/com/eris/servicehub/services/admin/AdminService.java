package com.eris.servicehub.services.admin;

import com.eris.servicehub.dtos.admin.UserSummaryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.UUID;

public interface AdminService {
    Page<UserSummaryResponse> getAllUsers(Pageable pageable);
    UserSummaryResponse setUserEnabledStatus(UUID userId, boolean enabled);
}