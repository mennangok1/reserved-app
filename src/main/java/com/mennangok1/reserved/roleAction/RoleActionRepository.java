package com.mennangok1.reserved.roleAction;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RoleActionRepository extends JpaRepository<RoleAction, Long> {

    boolean existsByRole_IdAndAction_Name(Long roleId, String actionName);

    Optional<RoleAction> findByRole_IdAndAction_Name(Long roleId, String actionName);

    @Query("SELECT ra FROM RoleAction ra JOIN FETCH ra.role JOIN FETCH ra.action")
    List<RoleAction> findAllWithRoleAndAction();

    @Query("SELECT ra FROM RoleAction ra JOIN FETCH ra.role r JOIN FETCH ra.action WHERE r.name = :roleName")
    List<RoleAction> findByRole_NameWithAction(@Param("roleName") String roleName);

}
