package com.mennangok1.reserved.restaurant;

import jakarta.validation.constraints.NotBlank;

public record CreateRestaurantRequest(
        @NotBlank(message = "Restaurant name cannot be empty")
        String name,

        String description
) {
}
