package com.mennangok1.reserved.roleAction;

import com.mennangok1.reserved.user.User;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

// RBAC: answers "can this role do X" (CLAUDE.md decision #2).
// Resource-based checks (e.g. "does this restaurant belong to this user") are a separate concern,
// done in each feature's service layer — see decision #1.
@Service
public class PermissionService {

    private final RoleActionRepository roleActionRepository;
    // Self-injected proxy: requirePermission must call hasPermission through this (not a plain
    // `this` call) or @Cacheable never goes through Spring's AOP proxy and silently never caches.
    private final PermissionService self;

    public PermissionService(RoleActionRepository roleActionRepository, @Lazy PermissionService self) {
        this.roleActionRepository = roleActionRepository;
        this.self = self;
    }

    @Cacheable(cacheNames = "rolePermissions", key = "#roleId + ':' + #actionName")
    public boolean hasPermission(Long roleId, String actionName) {
        return roleActionRepository.existsByRole_IdAndAction_Name(roleId, actionName);
    }

    public void requirePermission(User user, String actionName) {
        if (!self.hasPermission(user.getRole().getId(), actionName)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You don't have permission to do this");
        }
    }

    @CacheEvict(cacheNames = "rolePermissions", key = "#roleId + ':' + #actionName")
    public void evictPermissionCache(Long roleId, String actionName) {
    }

}
