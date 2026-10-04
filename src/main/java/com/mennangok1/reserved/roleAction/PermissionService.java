package com.mennangok1.reserved.roleAction;

import com.mennangok1.reserved.user.User;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

// RBAC: answers "can this role do X" (CLAUDE.md decision #2).
// Resource-based checks (e.g. "does this restaurant belong to this user") are a separate concern,
// done in each feature's service layer — see decision #1.
@Service
public class PermissionService {

    private final RoleActionRepository roleActionRepository;

    public PermissionService(RoleActionRepository roleActionRepository) {
        this.roleActionRepository = roleActionRepository;
    }

    @Cacheable(cacheNames = "rolePermissions", key = "#roleId + ':' + #actionName")
    public boolean hasPermission(Long roleId, String actionName) {
        return roleActionRepository.existsByRole_IdAndAction_Name(roleId, actionName);
    }

    public void requirePermission(User user, String actionName) {
        if (!hasPermission(user.getRole().getId(), actionName)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You don't have permission to do this");
        }
    }

}
