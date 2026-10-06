package com.mennangok1.reserved.reservation;

import com.mennangok1.reserved.customerUser.CustomerUser;
import com.mennangok1.reserved.customerUser.CustomerUserRepository;
import com.mennangok1.reserved.restaurantTable.RestaurantTable;
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
class ReservationServiceTest {

    @Mock
    private ReservationRepository reservationRepository;
    @Mock
    private TableHoldRepository tableHoldRepository;
    @Mock
    private CustomerUserRepository customerUserRepository;
    @Mock
    private PermissionService permissionService;

    @InjectMocks
    private ReservationService reservationService;

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

    private Reservation reservation(Long id, RestaurantTable table, CustomerUser customer, ReservationStatus status) {
        Reservation reservation = new Reservation();
        reservation.setId(id);
        reservation.setRestaurantTable(table);
        reservation.setCustomerUser(customer);
        reservation.setStatus(status);
        return reservation;
    }

    @Test
    void create_throwsForbidden_whenPermissionMissing() {
        User caller = user(1L);
        ReservationRequest request = new ReservationRequest(10L, LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(1).plusHours(2));

        doThrow(new ResponseStatusException(FORBIDDEN)).when(permissionService)
                .requirePermission(caller, "RESERVATION_CREATE");

        assertThatThrownBy(() -> reservationService.create(caller, request))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode().value())
                .isEqualTo(403);

        verify(customerUserRepository, never()).findByUser_Id(any());
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void create_throwsNotFound_whenCallerIsNotACustomer() {
        User caller = user(1L);
        ReservationRequest request = new ReservationRequest(10L, LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(1).plusHours(2));

        when(customerUserRepository.findByUser_Id(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reservationService.create(caller, request))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode().value())
                .isEqualTo(404);
    }

    @Test
    void create_throwsNotFound_whenHoldDoesNotExist() {
        User caller = user(1L);
        CustomerUser customer = customer(1L, caller);
        ReservationRequest request = new ReservationRequest(10L, LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(1).plusHours(2));

        when(customerUserRepository.findByUser_Id(1L)).thenReturn(Optional.of(customer));
        when(tableHoldRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reservationService.create(caller, request))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode().value())
                .isEqualTo(404);
    }

    @Test
    void create_throwsNotFound_whenHoldBelongsToAnotherCustomer() {
        User caller = user(1L);
        CustomerUser customer = customer(1L, caller);
        CustomerUser otherCustomer = customer(2L, user(2L));
        RestaurantTable table = table(100L);
        TableHold existingHold = hold(10L, table, otherCustomer, LocalDateTime.now().plusMinutes(3));
        ReservationRequest request = new ReservationRequest(10L, LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(1).plusHours(2));

        when(customerUserRepository.findByUser_Id(1L)).thenReturn(Optional.of(customer));
        when(tableHoldRepository.findById(10L)).thenReturn(Optional.of(existingHold));

        assertThatThrownBy(() -> reservationService.create(caller, request))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode().value())
                .isEqualTo(404);

        verify(reservationRepository, never()).save(any());
        verify(tableHoldRepository, never()).delete(any());
    }

    @Test
    void create_throwsConflict_whenHoldHasExpired() {
        User caller = user(1L);
        CustomerUser customer = customer(1L, caller);
        RestaurantTable table = table(100L);
        TableHold existingHold = hold(10L, table, customer, LocalDateTime.now().minusMinutes(1));
        ReservationRequest request = new ReservationRequest(10L, LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(1).plusHours(2));

        when(customerUserRepository.findByUser_Id(1L)).thenReturn(Optional.of(customer));
        when(tableHoldRepository.findById(10L)).thenReturn(Optional.of(existingHold));

        assertThatThrownBy(() -> reservationService.create(caller, request))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode().value())
                .isEqualTo(409);

        verify(reservationRepository, never()).save(any());
    }

    @Test
    void create_savesReservation_andConsumesHold() {
        User caller = user(1L);
        CustomerUser customer = customer(1L, caller);
        RestaurantTable table = table(100L);
        TableHold existingHold = hold(10L, table, customer, LocalDateTime.now().plusMinutes(3));
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        LocalDateTime end = start.plusHours(2);
        ReservationRequest request = new ReservationRequest(10L, start, end);

        when(customerUserRepository.findByUser_Id(1L)).thenReturn(Optional.of(customer));
        when(tableHoldRepository.findById(10L)).thenReturn(Optional.of(existingHold));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(invocation -> {
            Reservation saved = invocation.getArgument(0);
            saved.setId(50L);
            return saved;
        });

        ReservationResponse response = reservationService.create(caller, request);

        assertThat(response.id()).isEqualTo(50L);
        assertThat(response.tableId()).isEqualTo(100L);
        assertThat(response.status()).isEqualTo(ReservationStatus.ACTIVE);
        verify(tableHoldRepository).delete(existingHold);
    }

    @Test
    void listOwn_returnsOnlyCallersOwnReservations() {
        User caller = user(1L);
        CustomerUser customer = customer(1L, caller);
        RestaurantTable table = table(100L);

        when(customerUserRepository.findByUser_Id(1L)).thenReturn(Optional.of(customer));
        when(reservationRepository.findByCustomerUser_Id(1L))
                .thenReturn(List.of(reservation(50L, table, customer, ReservationStatus.ACTIVE)));

        List<ReservationResponse> response = reservationService.listOwn(caller);

        assertThat(response).hasSize(1);
        assertThat(response.get(0).id()).isEqualTo(50L);
    }

    @Test
    void cancel_throwsNotFound_whenReservationBelongsToAnotherCustomer() {
        User caller = user(1L);
        CustomerUser customer = customer(1L, caller);
        CustomerUser otherCustomer = customer(2L, user(2L));
        RestaurantTable table = table(100L);
        Reservation existing = reservation(50L, table, otherCustomer, ReservationStatus.ACTIVE);

        when(customerUserRepository.findByUser_Id(1L)).thenReturn(Optional.of(customer));
        when(reservationRepository.findById(50L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> reservationService.cancel(caller, 50L))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode().value())
                .isEqualTo(404);

        verify(reservationRepository, never()).save(any());
    }

    @Test
    void cancel_setsStatusToPassive_whenCallerOwnsIt() {
        User caller = user(1L);
        CustomerUser customer = customer(1L, caller);
        RestaurantTable table = table(100L);
        Reservation existing = reservation(50L, table, customer, ReservationStatus.ACTIVE);

        when(customerUserRepository.findByUser_Id(1L)).thenReturn(Optional.of(customer));
        when(reservationRepository.findById(50L)).thenReturn(Optional.of(existing));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        reservationService.cancel(caller, 50L);

        assertThat(existing.getStatus()).isEqualTo(ReservationStatus.PASSIVE);
        verify(reservationRepository).save(existing);
    }

}
