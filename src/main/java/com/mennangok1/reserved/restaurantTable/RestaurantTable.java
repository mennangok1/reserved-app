package com.mennangok1.reserved.restaurantTable;


import com.mennangok1.reserved.restaurant.Restaurant;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class RestaurantTable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column (name = "ID")
    private Long id;

    @Column (name = "CAPACITY")
    private Long capacity;

    @ManyToOne (fetch = FetchType.LAZY)
    @JoinColumn (name = "RESTAURANT_ID")
    private Restaurant restaurant;
}
