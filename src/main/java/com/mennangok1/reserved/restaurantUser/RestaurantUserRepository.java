package com.mennangok1.reserved.restaurantUser;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RestaurantUserRepository extends JpaRepository<RestaurantUser, Long> {

    Optional<RestaurantUser> findByUser_Id(Long userId);

    boolean existsByUser_Id(Long userId);

}
