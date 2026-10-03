package com.mennangok1.reserved.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "Email cannot be empty")
        @Email(message = "Enter a valid email address")
        String email,

        @NotBlank(message = "Password cannot be empty")
        String password
) {
}
