package com.mennangok1.reserved.reservation;

import com.mennangok1.reserved.customerUser.CustomerUser;
import com.mennangok1.reserved.restaurantTable.RestaurantTable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
public class Reservation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column (name = "ID")
    private Long id;

    @Column (name = "START_DATE")
    private LocalDateTime startDate;

    @Column (name = "END_DATE")
    private LocalDateTime endDate;

    @ManyToOne (fetch =  FetchType.LAZY)
    @JoinColumn (name = "TABLE_ID")
    private RestaurantTable restaurantTable;

    @ManyToOne (fetch =  FetchType.LAZY)
    @JoinColumn (name = "CUSTOMER_USER_ID")
    private CustomerUser customerUser;

    @Enumerated(EnumType.ORDINAL)
    @Column(name = "STATUS")
    private ReservationStatus status;



}
