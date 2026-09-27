package com.mennangok1.reserved.user;

import com.mennangok1.reserved.role.Role;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column (name = "ID")
    private Long id;

    @Column (name = "NAME", nullable = false)
    @NotBlank( message = "User Name cannot be empty!")
    private String name;

    @Column (name = "EMAIL", nullable = false)
    @Email (message = "Enter a valid e-mail address!")
    @NotBlank (message = "E-mail cannot be empty!")
    private String email;

    @Column (name = "PASSWORD", nullable = false)
    private String password;

    @Column (name = "PHONE_NUMBER")
    private String phoneNumber;

    @ManyToOne (fetch = FetchType.LAZY)
    @JoinColumn(name = "ROLE_ID", nullable = false)
    private Role role;

}
