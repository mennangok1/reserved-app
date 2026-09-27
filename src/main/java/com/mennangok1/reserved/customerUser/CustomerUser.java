package com.mennangok1.reserved.customerUser;

import com.mennangok1.reserved.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class CustomerUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column (name = "ID")
    private Long id;

    @OneToOne (fetch =  FetchType.LAZY)
    @JoinColumn (name = "USER_ID", nullable = false)
    private User user;
}
