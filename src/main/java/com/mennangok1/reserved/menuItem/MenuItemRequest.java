package com.mennangok1.reserved.menuItem;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record MenuItemRequest(
        @NotBlank(message = "Name cannot be empty")
        String name,

        String description,

        @NotNull(message = "Price is required")
        @Positive(message = "Price must be greater than zero")
        Double price,

        @PositiveOrZero(message = "Menu order cannot be negative")
        Long menuOrder,

        @NotNull(message = "Item type is required")
        Long itemTypeId
) {
}
