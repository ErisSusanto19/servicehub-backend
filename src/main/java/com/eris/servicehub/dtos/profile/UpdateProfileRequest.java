package com.eris.servicehub.dtos.profile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UpdateProfileRequest {

    @NotBlank(message = "Name cannot be blank")
    @Size(max = 255)
    private String name;

    @Size(max = 20, message = "Phone number cannot exceed 20 characters")
    private String phone;

    @Size(max = 255)
    private String address;

    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;
}