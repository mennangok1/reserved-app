package com.mennangok1.reserved.reservation;

import java.time.LocalDateTime;

public record ReservationResponse(
        Long id,
        Long tableId,
        LocalDateTime startDate,
        LocalDateTime endDate,
        ReservationStatus status
) {

    static ReservationResponse from(Reservation reservation) {
        return new ReservationResponse(
                reservation.getId(),
                reservation.getRestaurantTable().getId(),
                reservation.getStartDate(),
                reservation.getEndDate(),
                reservation.getStatus()
        );
    }

}
