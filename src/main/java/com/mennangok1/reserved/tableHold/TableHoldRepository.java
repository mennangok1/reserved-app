package com.mennangok1.reserved.tableHold;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TableHoldRepository extends JpaRepository<TableHold, Long> {

    List<TableHold> findByRestaurantTable_Id(Long tableId);

}
