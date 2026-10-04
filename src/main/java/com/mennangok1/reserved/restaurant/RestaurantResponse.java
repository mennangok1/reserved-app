package com.mennangok1.reserved.restaurant;

public record RestaurantResponse(
        Long id,
        String name,
        String description
) {

    static RestaurantResponse from(Restaurant restaurant) {
        return new RestaurantResponse(
                restaurant.getId(),
                restaurant.getName(),
                restaurant.getDescription()
        );
    }

}
