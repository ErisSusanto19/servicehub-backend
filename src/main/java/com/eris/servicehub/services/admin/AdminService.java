package com.eris.servicehub.services.admin;

import com.eris.servicehub.dtos.admin.UserSummaryResponse;
import com.eris.servicehub.dtos.service.ServiceResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.UUID;

public interface AdminService {
    Page<UserSummaryResponse> getAllUsers(Pageable pageable);
    UserSummaryResponse setUserEnabledStatus(UUID userId, boolean enabled);
    Page<ServiceResponse> getAllServices(Pageable pageable);
    void deleteService(UUID serviceId);
}