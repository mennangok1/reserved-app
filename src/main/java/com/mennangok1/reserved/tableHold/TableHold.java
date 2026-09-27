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

    @ManyToOne (fetch = FetchType.LAZY)
    @JoinColumn (name = "TABLE_ID", nullable = false)
    private RestaurantTable restaurantTable;

    @ManyToOne (fetch = FetchType.LAZY)
    @JoinColumn (name = "CUSTOMER_ID", nullable = false)
    private CustomerUser customerUser;


}
