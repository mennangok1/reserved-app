package com.mennangok1.reserved.restaurantTable;

import com.mennangok1.reserved.customerUser.CustomerUser;
import com.mennangok1.reserved.reservation.Reservation;
import com.mennangok1.reserved.reservation.ReservationRepository;
import com.mennangok1.reserved.reservation.ReservationStatus;
import com.mennangok1.reserved.restaurant.Restaurant;
import com.mennangok1.reserved.restaurant.RestaurantRepository;
import com.mennangok1.reserved.restaurantUser.RestaurantUser;
import com.mennangok1.reserved.restaurantUser.RestaurantUserRepository;
import com.mennangok1.reserved.roleAction.PermissionService;
import com.mennangok1.reserved.tableHold.TableHold;
import com.mennangok1.reserved.tableHold.TableHoldRepository;
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
class RestaurantTableServiceTest {

    @Mock
    private RestaurantTableRepository restaurantTableRepository;
    @Mock
    private RestaurantUserRepository restaurantUserRepository;
    @Mock
    private RestaurantRepository restaurantRepository;
    @Mock
    private TableHoldRepository tableHoldRepository;
    @Mock
    private ReservationRepository reservationRepository;
    @Mock
    private PermissionService permissionService;

    @InjectMocks
    private RestaurantTableService restaurantTableService;

    private User user(Long id) {
        User user = new User();
        user.setId(id);
        return user;
    }

    private Restaurant restaurant(Long id) {
        Restaurant restaurant = new Restaurant();
        restaurant.setId(id);
        return restaurant;
    }

    private RestaurantUser linkFor(User user, Restaurant restaurant) {
        RestaurantUser link = new RestaurantUser();
        link.setUser(user);
        link.setRestaurant(restaurant);
        return link;
    }

    private RestaurantTable restaurantTable(Long id, Restaurant restaurant) {
        RestaurantTable restaurantTable = new RestaurantTable();
        restaurantTable.setId(id);
        restaurantTable.setLabel("T1");
        restaurantTable.setCapacity(4L);
        restaurantTable.setRestaurant(restaurant);
        return restaurantTable;
    }

    private TableHold holdExpiringAt(LocalDateTime expiresAt) {
        TableHold hold = new TableHold();
        hold.setExpiresAt(expiresAt);
        return hold;
    }

    private Reservation reservationWithStatus(ReservationStatus status) {
        Reservation reservation = new Reservation();
        reservation.setStatus(status);
        return reservation;
    }

    @Test
    void create_throwsForbidden_whenPermissionMissing() {
        User customer = user(1L);
        RestaurantTableRequest request = new RestaurantTableRequest("T1", 4L);

        doThrow(new ResponseStatusException(FORBIDDEN)).when(permissionService)
                .requirePermission(customer, "RESTAURANT_TABLE_CREATE");

        assertThatThrownBy(() -> restaurantTableService.create(customer, request))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode().value())
                .isEqualTo(403);

        verify(restaurantUserRepository, never()).findByUser_Id(any());
        verify(restaurantTableRepository, never()).save(any());
    }

    @Test
    void create_throwsNotFound_whenCallerHasNoRestaurant() {
        User restaurantUser = user(1L);
        RestaurantTableRequest request = new RestaurantTableRequest("T1", 4L);

        when(restaurantUserRepository.findByUser_Id(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> restaurantTableService.create(restaurantUser, request))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode().value())
                .isEqualTo(404);
    }

    @Test
    void create_savesTableUnderCallersOwnRestaurant() {
        User restaurantUser = user(1L);
        Restaurant ownRestaurant = restaurant(10L);
        RestaurantTableRequest request = new RestaurantTableRequest("T1", 4L);

        when(restaurantUserRepository.findByUser_Id(1L)).thenReturn(Optional.of(linkFor(restaurantUser, ownRestaurant)));
        when(restaurantTableRepository.save(any(RestaurantTable.class))).thenAnswer(invocation -> {
            RestaurantTable saved = invocation.getArgument(0);
            saved.setId(100L);
            return saved;
        });

        RestaurantTableResponse response = restaurantTableService.create(restaurantUser, request);

        assertThat(response.id()).isEqualTo(100L);
        assertThat(response.label()).isEqualTo("T1");
        assertThat(response.capacity()).isEqualTo(4L);
        assertThat(response.restaurantId()).isEqualTo(10L);
    }

    @Test
    void listOwn_returnsOnlyCallersOwnRestaurantTables() {
        User restaurantUser = user(1L);
        Restaurant ownRestaurant = restaurant(10L);

        when(restaurantUserRepository.findByUser_Id(1L)).thenReturn(Optional.of(linkFor(restaurantUser, ownRestaurant)));
        when(restaurantTableRepository.findByRestaurant_Id(10L))
                .thenReturn(List.of(restaurantTable(100L, ownRestaurant)));

        List<RestaurantTableResponse> response = restaurantTableService.listOwn(restaurantUser);

        assertThat(response).hasSize(1);
        assertThat(response.get(0).restaurantId()).isEqualTo(10L);
    }

    @Test
    void update_throwsNotFound_whenTableBelongsToAnotherRestaurant() {
        User restaurantUserB = user(2L);
        Restaurant restaurantB = restaurant(20L);
        Restaurant restaurantA = restaurant(10L);
        RestaurantTable tableOwnedByA = restaurantTable(100L, restaurantA);
        RestaurantTableRequest request = new RestaurantTableRequest("T1", 4L);

        when(restaurantUserRepository.findByUser_Id(2L)).thenReturn(Optional.of(linkFor(restaurantUserB, restaurantB)));
        when(restaurantTableRepository.findById(100L)).thenReturn(Optional.of(tableOwnedByA));

        assertThatThrownBy(() -> restaurantTableService.update(restaurantUserB, 100L, request))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode().value())
                .isEqualTo(404);

        verify(restaurantTableRepository, never()).save(any());
    }

    @Test
    void update_updatesTable_whenCallerOwnsIt() {
        User restaurantUser = user(1L);
        Restaurant ownRestaurant = restaurant(10L);
        RestaurantTable existing = restaurantTable(100L, ownRestaurant);
        RestaurantTableRequest request = new RestaurantTableRequest("T2", 6L);

        when(restaurantUserRepository.findByUser_Id(1L)).thenReturn(Optional.of(linkFor(restaurantUser, ownRestaurant)));
        when(restaurantTableRepository.findById(100L)).thenReturn(Optional.of(existing));
        when(restaurantTableRepository.save(any(RestaurantTable.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RestaurantTableResponse response = restaurantTableService.update(restaurantUser, 100L, request);

        assertThat(response.label()).isEqualTo("T2");
        assertThat(response.capacity()).isEqualTo(6L);
    }

    @Test
    void delete_throwsNotFound_whenTableBelongsToAnotherRestaurant() {
        User restaurantUserB = user(2L);
        Restaurant restaurantB = restaurant(20L);
        Restaurant restaurantA = restaurant(10L);
        RestaurantTable tableOwnedByA = restaurantTable(100L, restaurantA);

        when(restaurantUserRepository.findByUser_Id(2L)).thenReturn(Optional.of(linkFor(restaurantUserB, restaurantB)));
        when(restaurantTableRepository.findById(100L)).thenReturn(Optional.of(tableOwnedByA));

        assertThatThrownBy(() -> restaurantTableService.delete(restaurantUserB, 100L))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode().value())
                .isEqualTo(404);

        verify(restaurantTableRepository, never()).delete(any());
    }

    @Test
    void delete_deletesTable_whenCallerOwnsIt() {
        User restaurantUser = user(1L);
        Restaurant ownRestaurant = restaurant(10L);
        RestaurantTable existing = restaurantTable(100L, ownRestaurant);

        when(restaurantUserRepository.findByUser_Id(1L)).thenReturn(Optional.of(linkFor(restaurantUser, ownRestaurant)));
        when(restaurantTableRepository.findById(100L)).thenReturn(Optional.of(existing));
        when(tableHoldRepository.findByRestaurantTable_Id(100L)).thenReturn(List.of());

        restaurantTableService.delete(restaurantUser, 100L);

        verify(restaurantTableRepository).delete(existing);
    }

    @Test
    void delete_throwsConflict_whenActiveHoldExists() {
        User restaurantUser = user(1L);
        Restaurant ownRestaurant = restaurant(10L);
        RestaurantTable existing = restaurantTable(100L, ownRestaurant);

        when(restaurantUserRepository.findByUser_Id(1L)).thenReturn(Optional.of(linkFor(restaurantUser, ownRestaurant)));
        when(restaurantTableRepository.findById(100L)).thenReturn(Optional.of(existing));
        when(tableHoldRepository.findByRestaurantTable_Id(100L))
                .thenReturn(List.of(holdExpiringAt(LocalDateTime.now().plusMinutes(5))));

        assertThatThrownBy(() -> restaurantTableService.delete(restaurantUser, 100L))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode().value())
                .isEqualTo(409);

        verify(restaurantTableRepository, never()).delete(any());
    }

    @Test
    void delete_succeeds_whenOnlyExpiredHoldExists() {
        User restaurantUser = user(1L);
        Restaurant ownRestaurant = restaurant(10L);
        RestaurantTable existing = restaurantTable(100L, ownRestaurant);

        when(restaurantUserRepository.findByUser_Id(1L)).thenReturn(Optional.of(linkFor(restaurantUser, ownRestaurant)));
        when(restaurantTableRepository.findById(100L)).thenReturn(Optional.of(existing));
        when(tableHoldRepository.findByRestaurantTable_Id(100L))
                .thenReturn(List.of(holdExpiringAt(LocalDateTime.now().minusMinutes(1))));

        restaurantTableService.delete(restaurantUser, 100L);

        verify(restaurantTableRepository).delete(existing);
    }

    @Test
    void delete_throwsConflict_whenActiveReservationExists() {
        User restaurantUser = user(1L);
        Restaurant ownRestaurant = restaurant(10L);
        RestaurantTable existing = restaurantTable(100L, ownRestaurant);

        when(restaurantUserRepository.findByUser_Id(1L)).thenReturn(Optional.of(linkFor(restaurantUser, ownRestaurant)));
        when(restaurantTableRepository.findById(100L)).thenReturn(Optional.of(existing));
        when(reservationRepository.findByRestaurantTable_Id(100L))
                .thenReturn(List.of(reservationWithStatus(ReservationStatus.ACTIVE)));

        assertThatThrownBy(() -> restaurantTableService.delete(restaurantUser, 100L))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode().value())
                .isEqualTo(409);

        verify(restaurantTableRepository, never()).delete(any());
    }

    @Test
    void delete_succeeds_whenOnlyCancelledReservationExists() {
        User restaurantUser = user(1L);
        Restaurant ownRestaurant = restaurant(10L);
        RestaurantTable existing = restaurantTable(100L, ownRestaurant);

        when(restaurantUserRepository.findByUser_Id(1L)).thenReturn(Optional.of(linkFor(restaurantUser, ownRestaurant)));
        when(restaurantTableRepository.findById(100L)).thenReturn(Optional.of(existing));
        when(reservationRepository.findByRestaurantTable_Id(100L))
                .thenReturn(List.of(reservationWithStatus(ReservationStatus.PASSIVE)));

        restaurantTableService.delete(restaurantUser, 100L);

        verify(restaurantTableRepository).delete(existing);
    }

    @Test
    void listByRestaurant_throwsForbidden_whenPermissionMissing() {
        User customer = user(1L);

        doThrow(new ResponseStatusException(FORBIDDEN)).when(permissionService)
                .requirePermission(customer, "RESTAURANT_READ");

        assertThatThrownBy(() -> restaurantTableService.listByRestaurant(customer, 10L))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode().value())
                .isEqualTo(403);

        verify(restaurantRepository, never()).findById(any());
    }

    @Test
    void listByRestaurant_throwsNotFound_whenRestaurantDoesNotExist() {
        User customer = user(1L);

        when(restaurantRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> restaurantTableService.listByRestaurant(customer, 10L))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode().value())
                .isEqualTo(404);
    }

    @Test
    void listByRestaurant_marksTableUnavailable_whenActiveHoldOrReservationExists() {
        User customer = user(1L);
        Restaurant restaurant = restaurant(10L);
        RestaurantTable heldTable = restaurantTable(100L, restaurant);
        RestaurantTable reservedTable = restaurantTable(101L, restaurant);
        RestaurantTable freeTable = restaurantTable(102L, restaurant);

        when(restaurantRepository.findById(10L)).thenReturn(Optional.of(restaurant));
        when(restaurantTableRepository.findByRestaurant_Id(10L))
                .thenReturn(List.of(heldTable, reservedTable, freeTable));
        when(tableHoldRepository.findByRestaurantTable_Id(100L))
                .thenReturn(List.of(holdExpiringAt(LocalDateTime.now().plusMinutes(5))));
        when(tableHoldRepository.findByRestaurantTable_Id(101L)).thenReturn(List.of());
        when(tableHoldRepository.findByRestaurantTable_Id(102L)).thenReturn(List.of());
        when(reservationRepository.findByRestaurantTable_Id(100L)).thenReturn(List.of());
        when(reservationRepository.findByRestaurantTable_Id(101L))
                .thenReturn(List.of(reservationWithStatus(ReservationStatus.ACTIVE)));
        when(reservationRepository.findByRestaurantTable_Id(102L)).thenReturn(List.of());

        List<RestaurantTableAvailabilityResponse> response = restaurantTableService.listByRestaurant(customer, 10L);

        assertThat(response).hasSize(3);
        assertThat(response).filteredOn(r -> r.id().equals(100L)).extracting(RestaurantTableAvailabilityResponse::available).containsExactly(false);
        assertThat(response).filteredOn(r -> r.id().equals(101L)).extracting(RestaurantTableAvailabilityResponse::available).containsExactly(false);
        assertThat(response).filteredOn(r -> r.id().equals(102L)).extracting(RestaurantTableAvailabilityResponse::available).containsExactly(true);
    }

}
