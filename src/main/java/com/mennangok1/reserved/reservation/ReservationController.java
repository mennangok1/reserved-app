package com.mennangok1.reserved.reservation;

import com.mennangok1.reserved.user.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/reservations")
class ReservationController {

    private final ReservationService reservationService;

    ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @PostMapping
    ResponseEntity<ReservationResponse> create(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ReservationRequest request
    ) {
        ReservationResponse response = reservationService.create(principal.getUser(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    ResponseEntity<List<ReservationResponse>> listOwn(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(reservationService.listOwn(principal.getUser()));
    }

    @DeleteMapping("/{id}")
    ResponseEntity<Void> cancel(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long id) {
        reservationService.cancel(principal.getUser(), id);
        return ResponseEntity.noContent().build();
    }

}
