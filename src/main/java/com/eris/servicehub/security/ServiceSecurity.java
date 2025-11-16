package com.eris.servicehub.security;

import com.eris.servicehub.entities.Service;
import com.eris.servicehub.repositories.ServiceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component("serviceSecurity")
public class ServiceSecurity {

    @Autowired
    private ServiceRepository serviceRepository;

    public boolean isOwner(Authentication authentication, UUID serviceId) {
        String currentUsername = authentication.getName();

        Service service = serviceRepository.findById(serviceId).orElse(null);
        if (service == null) {
            return false;
        }

        return service.getProvider().getEmail().equalsIgnoreCase(currentUsername);
    }
}