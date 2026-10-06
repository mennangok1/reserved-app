package com.mennangok1.reserved.tableHold;

import jakarta.validation.constraints.NotNull;

public record TableHoldRequest(
        @NotNull(message = "Table id is required")
        Long tableId
) {
}
