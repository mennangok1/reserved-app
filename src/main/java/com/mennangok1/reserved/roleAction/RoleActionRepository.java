package com.mennangok1.reserved.roleAction;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleActionRepository extends JpaRepository<RoleAction, Long> {

    boolean existsByRole_IdAndAction_Name(Long roleId, String actionName);

}
