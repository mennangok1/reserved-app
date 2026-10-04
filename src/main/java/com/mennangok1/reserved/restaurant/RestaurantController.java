package com.mennangok1.reserved.restaurant;

import com.mennangok1.reserved.user.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/restaurants")
class RestaurantController {

    private final RestaurantService restaurantService;

    RestaurantController(RestaurantService restaurantService) {
        this.restaurantService = restaurantService;
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

}
