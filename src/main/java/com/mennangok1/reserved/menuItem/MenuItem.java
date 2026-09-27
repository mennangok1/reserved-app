package com.mennangok1.reserved.menuItem;


import com.mennangok1.reserved.itemType.ItemType;
import com.mennangok1.reserved.restaurant.Restaurant;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class MenuItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column (name = "ID")
    private Long id;

    @Column (name = "NAME")
    private String name;

    @Column (name = "DESCRIPTION")
    private String description;

    @Column (name = "PRICE")
    private Double price;

    @Column (name = "MENU_ORDER")
    private Long menuOrder;

    @ManyToOne (fetch =  FetchType.LAZY)
    @JoinColumn (name = "ITEM_TYPE_ID", nullable = false)
    private ItemType itemType;

    @ManyToOne (fetch = FetchType.LAZY)
    @JoinColumn (name = "RESTAURANT_ID", nullable = false)
    private Restaurant restaurant;
}
