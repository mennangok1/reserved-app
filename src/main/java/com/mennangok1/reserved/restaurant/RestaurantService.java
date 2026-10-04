package com.mennangok1.reserved.restaurant;

import com.mennangok1.reserved.restaurantUser.RestaurantUser;
import com.mennangok1.reserved.restaurantUser.RestaurantUserRepository;
import com.mennangok1.reserved.user.User;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class RestaurantService {

    private static final String RESTAURANT_USER_ROLE = "RESTAURANT_USER";

    private final RestaurantRepository restaurantRepository;
    private final RestaurantUserRepository restaurantUserRepository;

    public RestaurantService(RestaurantRepository restaurantRepository, RestaurantUserRepository restaurantUserRepository) {
        this.restaurantRepository = restaurantRepository;
        this.restaurantUserRepository = restaurantUserRepository;
    }

    @Transactional
    public RestaurantResponse createOwnRestaurant(User currentUser, CreateRestaurantRequest request) {
        requireRestaurantUserRole(currentUser);

        if (restaurantUserRepository.existsByUser_Id(currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You already manage a restaurant");
        }

        Restaurant restaurant = new Restaurant();
        restaurant.setName(request.name());
        restaurant.setDescription(request.description());
        Restaurant savedRestaurant = restaurantRepository.save(restaurant);

        RestaurantUser link = new RestaurantUser();
        link.setUser(currentUser);
        link.setRestaurant(savedRestaurant);
        restaurantUserRepository.save(link);

        return RestaurantResponse.from(savedRestaurant);
    }

    public RestaurantResponse getOwnRestaurant(User currentUser) {
        requireRestaurantUserRole(currentUser);

        RestaurantUser link = restaurantUserRepository.findByUser_Id(currentUser.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "You don't manage a restaurant yet"));

        return RestaurantResponse.from(link.getRestaurant());
    }

    private void requireRestaurantUserRole(User user) {
        if (!RESTAURANT_USER_ROLE.equals(user.getRole().getName())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only restaurant users can do this");
        }
    }

}
