package com.mennangok1.reserved.restaurantTable;

public record RestaurantTableAvailabilityResponse(
        Long id,
        String label,
        Long capacity,
        boolean available
) {

    static RestaurantTableAvailabilityResponse from(RestaurantTable table, boolean available) {
        return new RestaurantTableAvailabilityResponse(
                table.getId(),
                table.getLabel(),
                table.getCapacity(),
                available
        );
    }

}
