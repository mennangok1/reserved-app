package com.mennangok1.reserved.role;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"NAME"}))
@Getter
@Setter
public class Role {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "NAME", nullable = false)
    private String name;



}
