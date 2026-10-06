package com.mennangok1.reserved.reservation;

import com.mennangok1.reserved.customerUser.CustomerUser;
import com.mennangok1.reserved.customerUser.CustomerUserRepository;
import com.mennangok1.reserved.roleAction.PermissionService;
import com.mennangok1.reserved.tableHold.TableHold;
import com.mennangok1.reserved.tableHold.TableHoldRepository;
import com.mennangok1.reserved.user.User;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final TableHoldRepository tableHoldRepository;
    private final CustomerUserRepository customerUserRepository;
    private final PermissionService permissionService;

    public ReservationService(
            ReservationRepository reservationRepository,
            TableHoldRepository tableHoldRepository,
            CustomerUserRepository customerUserRepository,
            PermissionService permissionService
    ) {
        this.reservationRepository = reservationRepository;
        this.tableHoldRepository = tableHoldRepository;
        this.customerUserRepository = customerUserRepository;
        this.permissionService = permissionService;
    }

    @Transactional
    public ReservationResponse create(User currentUser, ReservationRequest request) {
        permissionService.requirePermission(currentUser, "RESERVATION_CREATE");

        CustomerUser customer = resolveCustomer(currentUser);

        TableHold hold = tableHoldRepository.findById(request.holdId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Hold not found"));

        // 404 rather than 403 on mismatch: don't confirm another customer's hold even exists.
        if (!hold.getCustomerUser().getId().equals(customer.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Hold not found");
        }

        if (!hold.getExpiresAt().isAfter(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Hold has expired");
        }

        Reservation reservation = new Reservation();
        reservation.setRestaurantTable(hold.getRestaurantTable());
        reservation.setCustomerUser(customer);
        reservation.setStartDate(request.startDate());
        reservation.setEndDate(request.endDate());
        reservation.setStatus(ReservationStatus.ACTIVE);

        Reservation saved = reservationRepository.save(reservation);

        // Consumes the hold: creating a reservation is the only path that converts a hold into a booking.
        tableHoldRepository.delete(hold);

        return ReservationResponse.from(saved);
    }

    public List<ReservationResponse> listOwn(User currentUser) {
        permissionService.requirePermission(currentUser, "RESERVATION_READ");

        CustomerUser customer = resolveCustomer(currentUser);
        return reservationRepository.findByCustomerUser_Id(customer.getId()).stream()
                .map(ReservationResponse::from)
                .toList();
    }

    @Transactional
    public void cancel(User currentUser, Long reservationId) {
        permissionService.requirePermission(currentUser, "RESERVATION_CANCEL");

        CustomerUser customer = resolveCustomer(currentUser);

        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reservation not found"));

        // 404 rather than 403 on mismatch: don't confirm another customer's reservation even exists.
        if (!reservation.getCustomerUser().getId().equals(customer.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Reservation not found");
        }

        reservation.setStatus(ReservationStatus.PASSIVE);
        reservationRepository.save(reservation);
    }

    private CustomerUser resolveCustomer(User currentUser) {
        return customerUserRepository.findByUser_Id(currentUser.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "You are not registered as a customer"));
    }

}
