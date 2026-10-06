package com.mennangok1.reserved.restaurantTable;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record RestaurantTableRequest(
        @NotBlank(message = "Label cannot be empty")
        String label,

        @NotNull(message = "Capacity is required")
        @Positive(message = "Capacity must be greater than zero")
        Long capacity
) {
}
