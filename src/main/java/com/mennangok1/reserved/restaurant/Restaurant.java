package com.mennangok1.reserved.restaurant;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Restaurant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column (name = "ID")
    private Long id;

    @Column( name = "NAME")
    private String name;

    @Column (name = "DESCRIPTION")
    private String description;






}
