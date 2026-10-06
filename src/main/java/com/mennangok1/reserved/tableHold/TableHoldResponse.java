package com.mennangok1.reserved.tableHold;

import java.time.LocalDateTime;

public record TableHoldResponse(
        Long id,
        Long tableId,
        LocalDateTime expiresAt
) {

    static TableHoldResponse from(TableHold tableHold) {
        return new TableHoldResponse(
                tableHold.getId(),
                tableHold.getRestaurantTable().getId(),
                tableHold.getExpiresAt()
        );
    }

}
