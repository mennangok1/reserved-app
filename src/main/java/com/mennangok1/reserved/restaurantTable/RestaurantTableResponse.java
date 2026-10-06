package com.mennangok1.reserved.restaurantTable;

public record RestaurantTableResponse(
        Long id,
        String label,
        Long capacity,
        Long restaurantId
) {

    static RestaurantTableResponse from(RestaurantTable restaurantTable) {
        return new RestaurantTableResponse(
                restaurantTable.getId(),
                restaurantTable.getLabel(),
                restaurantTable.getCapacity(),
                restaurantTable.getRestaurant().getId()
        );
    }

}
