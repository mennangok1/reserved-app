package com.mennangok1.reserved.roleAction;

import jakarta.validation.constraints.NotBlank;

public record RoleActionGrantRequest(
        @NotBlank(message = "Role name cannot be empty") String roleName,
        @NotBlank(message = "Action name cannot be empty") String actionName
) {
}
