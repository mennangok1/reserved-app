package com.mennangok1.reserved.restaurantTable;

import com.mennangok1.reserved.restaurant.Restaurant;
import com.mennangok1.reserved.restaurantUser.RestaurantUserRepository;
import com.mennangok1.reserved.roleAction.PermissionService;
import com.mennangok1.reserved.user.User;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class RestaurantTableService {

    private final RestaurantTableRepository restaurantTableRepository;
    private final RestaurantUserRepository restaurantUserRepository;
    private final PermissionService permissionService;

    public RestaurantTableService(
            RestaurantTableRepository restaurantTableRepository,
            RestaurantUserRepository restaurantUserRepository,
            PermissionService permissionService
    ) {
        this.restaurantTableRepository = restaurantTableRepository;
        this.restaurantUserRepository = restaurantUserRepository;
        this.permissionService = permissionService;
    }

    @Transactional
    public RestaurantTableResponse create(User currentUser, RestaurantTableRequest request) {
        permissionService.requirePermission(currentUser, "RESTAURANT_TABLE_CREATE");

        Restaurant ownRestaurant = resolveOwnRestaurant(currentUser);

        RestaurantTable restaurantTable = new RestaurantTable();
        restaurantTable.setLabel(request.label());
        restaurantTable.setCapacity(request.capacity());
        restaurantTable.setRestaurant(ownRestaurant);

        RestaurantTable saved = restaurantTableRepository.save(restaurantTable);
        return RestaurantTableResponse.from(saved);
    }

    public List<RestaurantTableResponse> listOwn(User currentUser) {
        permissionService.requirePermission(currentUser, "RESTAURANT_TABLE_READ");

        Restaurant ownRestaurant = resolveOwnRestaurant(currentUser);
        return restaurantTableRepository.findByRestaurant_Id(ownRestaurant.getId()).stream()
                .map(RestaurantTableResponse::from)
                .toList();
    }

    @Transactional
    public RestaurantTableResponse update(User currentUser, Long tableId, RestaurantTableRequest request) {
        permissionService.requirePermission(currentUser, "RESTAURANT_TABLE_UPDATE");

        Restaurant ownRestaurant = resolveOwnRestaurant(currentUser);
        RestaurantTable restaurantTable = resolveOwnedTable(tableId, ownRestaurant.getId());

        restaurantTable.setLabel(request.label());
        restaurantTable.setCapacity(request.capacity());

        RestaurantTable saved = restaurantTableRepository.save(restaurantTable);
        return RestaurantTableResponse.from(saved);
    }

    @Transactional
    public void delete(User currentUser, Long tableId) {
        permissionService.requirePermission(currentUser, "RESTAURANT_TABLE_DELETE");

        Restaurant ownRestaurant = resolveOwnRestaurant(currentUser);
        RestaurantTable restaurantTable = resolveOwnedTable(tableId, ownRestaurant.getId());

        restaurantTableRepository.delete(restaurantTable);
    }

    private Restaurant resolveOwnRestaurant(User currentUser) {
        return restaurantUserRepository.findByUser_Id(currentUser.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "You don't manage a restaurant yet"))
                .getRestaurant();
    }

    private RestaurantTable resolveOwnedTable(Long tableId, Long ownRestaurantId) {
        RestaurantTable restaurantTable = restaurantTableRepository.findById(tableId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Table not found"));

        // 404 rather than 403 on mismatch: don't confirm another restaurant's table even exists.
        if (!restaurantTable.getRestaurant().getId().equals(ownRestaurantId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Table not found");
        }

        return restaurantTable;
    }

}
