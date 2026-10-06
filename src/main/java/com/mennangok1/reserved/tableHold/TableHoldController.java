package com.mennangok1.reserved.tableHold;

import com.mennangok1.reserved.user.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/table-holds")
class TableHoldController {

    private final TableHoldService tableHoldService;

    TableHoldController(TableHoldService tableHoldService) {
        this.tableHoldService = tableHoldService;
    }

    @PostMapping
    ResponseEntity<TableHoldResponse> create(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody TableHoldRequest request
    ) {
        TableHoldResponse response = tableHoldService.create(principal.getUser(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/{id}")
    ResponseEntity<Void> release(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long id) {
        tableHoldService.release(principal.getUser(), id);
        return ResponseEntity.noContent().build();
    }

}
