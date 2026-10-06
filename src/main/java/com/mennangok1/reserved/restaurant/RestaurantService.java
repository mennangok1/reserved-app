package com.mennangok1.reserved.restaurant;

import com.mennangok1.reserved.restaurantUser.RestaurantUser;
import com.mennangok1.reserved.restaurantUser.RestaurantUserRepository;
import com.mennangok1.reserved.roleAction.PermissionService;
import com.mennangok1.reserved.user.User;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class RestaurantService {

    private static final String RESTAURANT_USER_ROLE = "RESTAURANT_USER";

    private final RestaurantRepository restaurantRepository;
    private final RestaurantUserRepository restaurantUserRepository;
    private final PermissionService permissionService;

    public RestaurantService(
            RestaurantRepository restaurantRepository,
            RestaurantUserRepository restaurantUserRepository,
            PermissionService permissionService
    ) {
        this.restaurantRepository = restaurantRepository;
        this.restaurantUserRepository = restaurantUserRepository;
        this.permissionService = permissionService;
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

    public List<RestaurantResponse> listRestaurants(User currentUser, String name) {
        permissionService.requirePermission(currentUser, "RESTAURANT_READ");

        List<Restaurant> restaurants = (name == null || name.isBlank())
                ? restaurantRepository.findAll()
                : restaurantRepository.findByNameContainingIgnoreCase(name);

        return restaurants.stream().map(RestaurantResponse::from).toList();
    }

    public RestaurantResponse getRestaurant(User currentUser, Long restaurantId) {
        permissionService.requirePermission(currentUser, "RESTAURANT_READ");

        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Restaurant not found"));

        return RestaurantResponse.from(restaurant);
    }

    private void requireRestaurantUserRole(User user) {
        if (!RESTAURANT_USER_ROLE.equals(user.getRole().getName())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only restaurant users can do this");
        }
    }

}
