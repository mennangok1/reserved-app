package com.mennangok1.reserved.error;

import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

// Project's first global exception handler. Every other error path uses a deliberately thrown
// ResponseStatusException in service code; this is a safety net for the ones that don't go
// through a pre-check, e.g. deleting a restaurant_table that a PASSIVE (cancelled) reservation
// row still references — reservation.table_id has no ON DELETE clause (see ADR-007).
//
// Catches the broad DataAccessException, not just DataIntegrityViolationException: in practice
// Hibernate rejects this particular case as an InvalidDataAccessApiUsageException
// (TransientPropertyValueException) during flush, before Postgres ever gets a chance to raise
// the raw FK violation — both are the same class of "conflicts with existing data" problem from
// the caller's point of view.
@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(DataAccessException.class)
    ResponseEntity<Map<String, String>> handleDataAccessException(DataAccessException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("message", "This operation conflicts with existing data"));
    }

}
