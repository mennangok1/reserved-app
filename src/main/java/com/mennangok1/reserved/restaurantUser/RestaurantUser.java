package com.mennangok1.reserved.restaurantUser;


import com.mennangok1.reserved.restaurant.Restaurant;
import com.mennangok1.reserved.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class RestaurantUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column (name = "ID")
    private Long id;

    @OneToOne (fetch = FetchType.LAZY)
    @JoinColumn (name = "USER_ID")
    private User user;

    @ManyToOne (fetch = FetchType.LAZY)
    @JoinColumn (name = "RESTAURANT_ID")
    private Restaurant restaurant;
}
