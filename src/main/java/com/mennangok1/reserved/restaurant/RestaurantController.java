package com.mennangok1.reserved.restaurant;

import com.mennangok1.reserved.menuItem.MenuItemResponse;
import com.mennangok1.reserved.menuItem.MenuItemService;
import com.mennangok1.reserved.user.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/restaurants")
class RestaurantController {

    private final RestaurantService restaurantService;
    private final MenuItemService menuItemService;

    RestaurantController(RestaurantService restaurantService, MenuItemService menuItemService) {
        this.restaurantService = restaurantService;
        this.menuItemService = menuItemService;
    }

    @PostMapping
    ResponseEntity<RestaurantResponse> createOwnRestaurant(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateRestaurantRequest request
    ) {
        RestaurantResponse response = restaurantService.createOwnRestaurant(principal.getUser(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/mine")
    ResponseEntity<RestaurantResponse> getOwnRestaurant(@AuthenticationPrincipal UserPrincipal principal) {
        RestaurantResponse response = restaurantService.getOwnRestaurant(principal.getUser());
        return ResponseEntity.ok(response);
    }

    @GetMapping
    ResponseEntity<List<RestaurantResponse>> listRestaurants(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String name
    ) {
        return ResponseEntity.ok(restaurantService.listRestaurants(principal.getUser(), name));
    }

    @GetMapping("/{id}")
    ResponseEntity<RestaurantResponse> getRestaurant(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(restaurantService.getRestaurant(principal.getUser(), id));
    }

    @GetMapping("/{id}/menu-items")
    ResponseEntity<List<MenuItemResponse>> listMenuItems(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(menuItemService.listByRestaurant(principal.getUser(), id));
    }

}
