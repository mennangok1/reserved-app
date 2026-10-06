package com.mennangok1.reserved.tableHold;

import com.mennangok1.reserved.customerUser.CustomerUser;
import com.mennangok1.reserved.customerUser.CustomerUserRepository;
import com.mennangok1.reserved.restaurantTable.RestaurantTable;
import com.mennangok1.reserved.restaurantTable.RestaurantTableRepository;
import com.mennangok1.reserved.roleAction.PermissionService;
import com.mennangok1.reserved.user.User;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TableHoldService {

    private final TableHoldRepository tableHoldRepository;
    private final RestaurantTableRepository restaurantTableRepository;
    private final CustomerUserRepository customerUserRepository;
    private final PermissionService permissionService;

    public TableHoldService(
            TableHoldRepository tableHoldRepository,
            RestaurantTableRepository restaurantTableRepository,
            CustomerUserRepository customerUserRepository,
            PermissionService permissionService
    ) {
        this.tableHoldRepository = tableHoldRepository;
        this.restaurantTableRepository = restaurantTableRepository;
        this.customerUserRepository = customerUserRepository;
        this.permissionService = permissionService;
    }

    @Transactional
    public TableHoldResponse create(User currentUser, TableHoldRequest request) {
        permissionService.requirePermission(currentUser, "TABLE_HOLD_CREATE");

        CustomerUser customer = resolveCustomer(currentUser);

        RestaurantTable restaurantTable = restaurantTableRepository.findById(request.tableId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Table not found"));

        LocalDateTime now = LocalDateTime.now();
        boolean activeHoldExists = tableHoldRepository.findByRestaurantTable_Id(restaurantTable.getId()).stream()
                .anyMatch(hold -> hold.getExpiresAt().isAfter(now));
        if (activeHoldExists) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Table is currently held");
        }

        TableHold tableHold = new TableHold();
        tableHold.setRestaurantTable(restaurantTable);
        tableHold.setCustomerUser(customer);
        tableHold.setExpiresAt(now.plusMinutes(5));

        TableHold saved = tableHoldRepository.save(tableHold);
        return TableHoldResponse.from(saved);
    }

    @Transactional
    public void release(User currentUser, Long holdId) {
        permissionService.requirePermission(currentUser, "TABLE_HOLD_DELETE");

        CustomerUser customer = resolveCustomer(currentUser);

        TableHold tableHold = tableHoldRepository.findById(holdId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Hold not found"));

        // 404 rather than 403 on mismatch: don't confirm another customer's hold even exists.
        if (!tableHold.getCustomerUser().getId().equals(customer.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Hold not found");
        }

        tableHoldRepository.delete(tableHold);
    }

    private CustomerUser resolveCustomer(User currentUser) {
        return customerUserRepository.findByUser_Id(currentUser.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "You are not registered as a customer"));
    }

}
