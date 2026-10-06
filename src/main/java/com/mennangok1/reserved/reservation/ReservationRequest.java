package com.mennangok1.reserved.reservation;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record ReservationRequest(
        @NotNull(message = "Hold id is required")
        Long holdId,

        @NotNull(message = "Start date is required")
        LocalDateTime startDate,

        @NotNull(message = "End date is required")
        LocalDateTime endDate
) {

    @AssertTrue(message = "End date must be after start date")
    public boolean isEndAfterStart() {
        return startDate == null || endDate == null || endDate.isAfter(startDate);
    }

}
