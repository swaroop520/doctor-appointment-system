package com.project.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class OtpLoginRequest {
    @NotBlank
    private String mobileNumber;

    @NotBlank
    private String pin;
}
