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
        // restaurantTable can be null (V13) for a historical reservation whose table was later
        // deleted — don't NPE when listing a customer's own past reservations.
        Long tableId = reservation.getRestaurantTable() != null ? reservation.getRestaurantTable().getId() : null;
        return new ReservationResponse(
                reservation.getId(),
                tableId,
                reservation.getStartDate(),
                reservation.getEndDate(),
                reservation.getStatus()
        );
    }

}
