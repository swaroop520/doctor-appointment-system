package com.project.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {
    @NotBlank
    private String identifier; // Can be an email OR a mobile number

    @NotBlank
    private String password; // Can be the actual password OR the static PIN
}
