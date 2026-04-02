package com.project.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SignupRequest {
    @NotBlank
    private String name;

    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String password;

    @NotBlank
    private String role; // ADMIN, DOCTOR, PATIENT

    private String mobileNumber;

    // For Doctors only
    private String specialization;
    private String licenseNumber;
}
