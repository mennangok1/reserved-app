package com.mennangok1.reserved.restaurantTable;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RestaurantTableRepository extends JpaRepository<RestaurantTable, Long> {

    List<RestaurantTable> findByRestaurant_Id(Long restaurantId);

}
