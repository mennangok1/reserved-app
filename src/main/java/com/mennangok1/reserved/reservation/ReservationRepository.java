package com.mennangok1.reserved.reservation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    List<Reservation> findByRestaurantTable_Id(Long tableId);

    List<Reservation> findByCustomerUser_Id(Long customerUserId);

    List<Reservation> findByRestaurantTable_Restaurant_Id(Long restaurantId);

    // Classic interval-overlap predicate (start < otherEnd AND end > otherStart), scoped to ACTIVE
    // reservations only — a cancelled (PASSIVE) reservation never conflicts.
    @Query("SELECT COUNT(r) > 0 FROM Reservation r " +
            "WHERE r.restaurantTable.id = :tableId " +
            "AND r.status = com.mennangok1.reserved.reservation.ReservationStatus.ACTIVE " +
            "AND r.startDate < :endDate AND r.endDate > :startDate")
    boolean existsOverlapping(@Param("tableId") Long tableId,
                               @Param("startDate") LocalDateTime startDate,
                               @Param("endDate") LocalDateTime endDate);

}
