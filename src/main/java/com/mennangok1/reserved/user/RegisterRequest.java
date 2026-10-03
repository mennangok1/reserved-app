package com.mennangok1.reserved.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Name cannot be empty")
        String name,

        @NotBlank(message = "Email cannot be empty")
        @Email(message = "Enter a valid email address")
        String email,

        @NotBlank(message = "Password cannot be empty")
        @Size(min = 8, message = "Password must be at least 8 characters")
        String password,

        String phoneNumber
) {
}
