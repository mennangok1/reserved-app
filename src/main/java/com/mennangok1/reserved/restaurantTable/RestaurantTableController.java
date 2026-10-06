package com.mennangok1.reserved.restaurantTable;

import com.mennangok1.reserved.user.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/restaurant-tables")
class RestaurantTableController {

    private final RestaurantTableService restaurantTableService;

    RestaurantTableController(RestaurantTableService restaurantTableService) {
        this.restaurantTableService = restaurantTableService;
    }

    @PostMapping
    ResponseEntity<RestaurantTableResponse> create(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody RestaurantTableRequest request
    ) {
        RestaurantTableResponse response = restaurantTableService.create(principal.getUser(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    ResponseEntity<List<RestaurantTableResponse>> listOwn(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(restaurantTableService.listOwn(principal.getUser()));
    }

    @PutMapping("/{id}")
    ResponseEntity<RestaurantTableResponse> update(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody RestaurantTableRequest request
    ) {
        RestaurantTableResponse response = restaurantTableService.update(principal.getUser(), id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long id) {
        restaurantTableService.delete(principal.getUser(), id);
        return ResponseEntity.noContent().build();
    }

}
