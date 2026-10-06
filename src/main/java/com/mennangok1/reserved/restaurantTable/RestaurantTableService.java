package com.mennangok1.reserved.restaurantTable;

import com.mennangok1.reserved.reservation.ReservationRepository;
import com.mennangok1.reserved.reservation.ReservationStatus;
import com.mennangok1.reserved.restaurant.Restaurant;
import com.mennangok1.reserved.restaurant.RestaurantRepository;
import com.mennangok1.reserved.restaurantUser.RestaurantUserRepository;
import com.mennangok1.reserved.roleAction.PermissionService;
import com.mennangok1.reserved.tableHold.TableHoldRepository;
import com.mennangok1.reserved.user.User;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class RestaurantTableService {

    private final RestaurantTableRepository restaurantTableRepository;
    private final RestaurantUserRepository restaurantUserRepository;
    private final RestaurantRepository restaurantRepository;
    private final TableHoldRepository tableHoldRepository;
    private final ReservationRepository reservationRepository;
    private final PermissionService permissionService;

    public RestaurantTableService(
            RestaurantTableRepository restaurantTableRepository,
            RestaurantUserRepository restaurantUserRepository,
            RestaurantRepository restaurantRepository,
            TableHoldRepository tableHoldRepository,
            ReservationRepository reservationRepository,
            PermissionService permissionService
    ) {
        this.restaurantTableRepository = restaurantTableRepository;
        this.restaurantUserRepository = restaurantUserRepository;
        this.restaurantRepository = restaurantRepository;
        this.tableHoldRepository = tableHoldRepository;
        this.reservationRepository = reservationRepository;
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

        boolean hasActiveHold = tableHoldRepository.findByRestaurantTable_Id(restaurantTable.getId()).stream()
                .anyMatch(hold -> hold.getExpiresAt().isAfter(LocalDateTime.now()));
        if (hasActiveHold) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Table has an active hold and cannot be deleted");
        }

        boolean hasActiveReservation = reservationRepository.findByRestaurantTable_Id(restaurantTable.getId()).stream()
                .anyMatch(reservation -> reservation.getStatus() == ReservationStatus.ACTIVE);
        if (hasActiveReservation) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Table has an active reservation and cannot be deleted");
        }

        restaurantTableRepository.delete(restaurantTable);
    }

    // Customer-facing: lets a CUSTOMER discover a table to hold/reserve. Reuses RESTAURANT_READ
    // (not a RESTAURANT_TABLE_* action) since this is a resource-scoped "view this restaurant" read,
    // the same reasoning ADR-004 used for MenuItemService.listByRestaurant.
    public List<RestaurantTableAvailabilityResponse> listByRestaurant(User currentUser, Long restaurantId) {
        permissionService.requirePermission(currentUser, "RESTAURANT_READ");

        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Restaurant not found"));

        LocalDateTime now = LocalDateTime.now();
        return restaurantTableRepository.findByRestaurant_Id(restaurant.getId()).stream()
                .map(table -> RestaurantTableAvailabilityResponse.from(table, isAvailable(table, now)))
                .toList();
    }

    // "Available" means right now, not for a requested time slot — the reservation system has no
    // date-range availability query yet (see ADR-007's known gaps).
    private boolean isAvailable(RestaurantTable table, LocalDateTime now) {
        boolean hasActiveHold = tableHoldRepository.findByRestaurantTable_Id(table.getId()).stream()
                .anyMatch(hold -> hold.getExpiresAt().isAfter(now));
        boolean hasActiveReservation = reservationRepository.findByRestaurantTable_Id(table.getId()).stream()
                .anyMatch(reservation -> reservation.getStatus() == ReservationStatus.ACTIVE);
        return !hasActiveHold && !hasActiveReservation;
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
