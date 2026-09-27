package com.mennangok1.reserved.roleAction;

import com.mennangok1.reserved.action.Action;
import com.mennangok1.reserved.role.Role;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"ROLE_ID", "ACTION_ID"}))
@Getter
@Setter
public class RoleAction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @ManyToOne (fetch =  FetchType.LAZY)
    @JoinColumn(name = "ROLE_ID", nullable = false)
    private Role role;

    @ManyToOne (fetch =  FetchType.LAZY)
    @JoinColumn(name = "ACTION_ID", nullable = false)
    private Action action;


}
