package com.eris.servicehub.dtos.profile;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
public class UserProfileResponse {
    private UUID id;
    private String name;
    private String email;
    private String image;
    private String phone;
    private String address;
    private LocalDate dateOfBirth;
}