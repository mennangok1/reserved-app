package com.mennangok1.reserved.roleAction;

public record RoleActionResponse(
        Long id,
        String roleName,
        String actionName
) {

    static RoleActionResponse from(RoleAction roleAction) {
        return new RoleActionResponse(
                roleAction.getId(),
                roleAction.getRole().getName(),
                roleAction.getAction().getName()
        );
    }

}
