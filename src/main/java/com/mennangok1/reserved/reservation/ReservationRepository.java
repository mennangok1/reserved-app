package com.mennangok1.reserved.reservation;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    List<Reservation> findByRestaurantTable_Id(Long tableId);

    List<Reservation> findByCustomerUser_Id(Long customerUserId);

}
