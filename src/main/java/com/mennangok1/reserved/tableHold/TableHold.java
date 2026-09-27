package com.mennangok1.reserved.tableHold;

import com.mennangok1.reserved.customerUser.CustomerUser;
import com.mennangok1.reserved.restaurantTable.RestaurantTable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
public class TableHold {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column (name = "EXPIRES_AT")
    private LocalDateTime expiresAt;

    @ManyToOne
    @JoinColumn (name = "TABLE_ID")
    private RestaurantTable restaurantTable;

    @ManyToOne
    @JoinColumn (name = "CUSTOMER_ID")
    private CustomerUser customerUser;


}
