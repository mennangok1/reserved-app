package com.mennangok1.reserved.tableHold;

import com.mennangok1.reserved.customerUser.CustomerUser;
import com.mennangok1.reserved.customerUser.CustomerUserRepository;
import com.mennangok1.reserved.restaurantTable.RestaurantTable;
import com.mennangok1.reserved.restaurantTable.RestaurantTableRepository;
import com.mennangok1.reserved.roleAction.PermissionService;
import com.mennangok1.reserved.user.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.FORBIDDEN;

@ExtendWith(MockitoExtension.class)
class TableHoldServiceTest {

    @Mock
    private TableHoldRepository tableHoldRepository;
    @Mock
    private RestaurantTableRepository restaurantTableRepository;
    @Mock
    private CustomerUserRepository customerUserRepository;
    @Mock
    private PermissionService permissionService;

    @InjectMocks
    private TableHoldService tableHoldService;

    private User user(Long id) {
        User user = new User();
        user.setId(id);
        return user;
    }

    private CustomerUser customer(Long id, User user) {
        CustomerUser customer = new CustomerUser();
        customer.setId(id);
        customer.setUser(user);
        return customer;
    }

    private RestaurantTable table(Long id) {
        RestaurantTable table = new RestaurantTable();
        table.setId(id);
        table.setLabel("T1");
        table.setCapacity(4L);
        return table;
    }

    private TableHold hold(Long id, RestaurantTable table, CustomerUser customer, LocalDateTime expiresAt) {
        TableHold hold = new TableHold();
        hold.setId(id);
        hold.setRestaurantTable(table);
        hold.setCustomerUser(customer);
        hold.setExpiresAt(expiresAt);
        return hold;
    }

    @Test
    void create_throwsForbidden_whenPermissionMissing() {
        User caller = user(1L);
        TableHoldRequest request = new TableHoldRequest(100L);

        doThrow(new ResponseStatusException(FORBIDDEN)).when(permissionService)
                .requirePermission(caller, "TABLE_HOLD_CREATE");

        assertThatThrownBy(() -> tableHoldService.create(caller, request))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode().value())
                .isEqualTo(403);

        verify(customerUserRepository, never()).findByUser_Id(any());
        verify(tableHoldRepository, never()).save(any());
    }

    @Test
    void create_throwsNotFound_whenCallerIsNotACustomer() {
        User caller = user(1L);
        TableHoldRequest request = new TableHoldRequest(100L);

        when(customerUserRepository.findByUser_Id(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tableHoldService.create(caller, request))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode().value())
                .isEqualTo(404);
    }

    @Test
    void create_throwsNotFound_whenTableDoesNotExist() {
        User caller = user(1L);
        CustomerUser customer = customer(1L, caller);
        TableHoldRequest request = new TableHoldRequest(100L);

        when(customerUserRepository.findByUser_Id(1L)).thenReturn(Optional.of(customer));
        when(restaurantTableRepository.findById(100L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tableHoldService.create(caller, request))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode().value())
                .isEqualTo(404);
    }

    @Test
    void create_throwsConflict_whenTableAlreadyActivelyHeld() {
        User caller = user(1L);
        CustomerUser customer = customer(1L, caller);
        RestaurantTable table = table(100L);
        TableHoldRequest request = new TableHoldRequest(100L);

        when(customerUserRepository.findByUser_Id(1L)).thenReturn(Optional.of(customer));
        when(restaurantTableRepository.findById(100L)).thenReturn(Optional.of(table));
        when(tableHoldRepository.findByRestaurantTable_Id(100L))
                .thenReturn(List.of(hold(1L, table, customer(2L, user(2L)), LocalDateTime.now().plusMinutes(3))));

        assertThatThrownBy(() -> tableHoldService.create(caller, request))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode().value())
                .isEqualTo(409);

        verify(tableHoldRepository, never()).save(any());
    }

    @Test
    void create_succeeds_whenPriorHoldOnTableHasExpired() {
        User caller = user(1L);
        CustomerUser customer = customer(1L, caller);
        RestaurantTable table = table(100L);
        TableHoldRequest request = new TableHoldRequest(100L);

        when(customerUserRepository.findByUser_Id(1L)).thenReturn(Optional.of(customer));
        when(restaurantTableRepository.findById(100L)).thenReturn(Optional.of(table));
        when(tableHoldRepository.findByRestaurantTable_Id(100L))
                .thenReturn(List.of(hold(1L, table, customer(2L, user(2L)), LocalDateTime.now().minusMinutes(1))));
        when(tableHoldRepository.save(any(TableHold.class))).thenAnswer(invocation -> {
            TableHold saved = invocation.getArgument(0);
            saved.setId(50L);
            return saved;
        });

        TableHoldResponse response = tableHoldService.create(caller, request);

        assertThat(response.id()).isEqualTo(50L);
        assertThat(response.tableId()).isEqualTo(100L);
    }

    @Test
    void create_savesHold_expiringFiveMinutesFromNow() {
        User caller = user(1L);
        CustomerUser customer = customer(1L, caller);
        RestaurantTable table = table(100L);
        TableHoldRequest request = new TableHoldRequest(100L);

        when(customerUserRepository.findByUser_Id(1L)).thenReturn(Optional.of(customer));
        when(restaurantTableRepository.findById(100L)).thenReturn(Optional.of(table));
        when(tableHoldRepository.findByRestaurantTable_Id(100L)).thenReturn(List.of());
        when(tableHoldRepository.save(any(TableHold.class))).thenAnswer(invocation -> {
            TableHold saved = invocation.getArgument(0);
            saved.setId(50L);
            return saved;
        });

        LocalDateTime before = LocalDateTime.now();
        TableHoldResponse response = tableHoldService.create(caller, request);
        LocalDateTime after = LocalDateTime.now();

        assertThat(response.expiresAt()).isAfter(before.plusMinutes(4));
        assertThat(response.expiresAt()).isBefore(after.plusMinutes(6));
    }

    @Test
    void release_throwsForbidden_whenPermissionMissing() {
        User caller = user(1L);

        doThrow(new ResponseStatusException(FORBIDDEN)).when(permissionService)
                .requirePermission(caller, "TABLE_HOLD_DELETE");

        assertThatThrownBy(() -> tableHoldService.release(caller, 1L))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode().value())
                .isEqualTo(403);

        verify(tableHoldRepository, never()).delete(any());
    }

    @Test
    void release_throwsNotFound_whenHoldDoesNotExist() {
        User caller = user(1L);
        CustomerUser customer = customer(1L, caller);

        when(customerUserRepository.findByUser_Id(1L)).thenReturn(Optional.of(customer));
        when(tableHoldRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tableHoldService.release(caller, 1L))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode().value())
                .isEqualTo(404);
    }

    @Test
    void release_throwsNotFound_whenHoldBelongsToAnotherCustomer() {
        User callerUser = user(1L);
        CustomerUser caller = customer(1L, callerUser);
        CustomerUser otherCustomer = customer(2L, user(2L));
        TableHold existing = hold(1L, table(100L), otherCustomer, LocalDateTime.now().plusMinutes(3));

        when(customerUserRepository.findByUser_Id(1L)).thenReturn(Optional.of(caller));
        when(tableHoldRepository.findById(1L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> tableHoldService.release(callerUser, 1L))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode().value())
                .isEqualTo(404);

        verify(tableHoldRepository, never()).delete(any());
    }

    @Test
    void release_deletesHold_whenCallerOwnsIt() {
        User callerUser = user(1L);
        CustomerUser caller = customer(1L, callerUser);
        TableHold existing = hold(1L, table(100L), caller, LocalDateTime.now().plusMinutes(3));

        when(customerUserRepository.findByUser_Id(1L)).thenReturn(Optional.of(caller));
        when(tableHoldRepository.findById(1L)).thenReturn(Optional.of(existing));

        tableHoldService.release(callerUser, 1L);

        verify(tableHoldRepository).delete(existing);
    }

}
