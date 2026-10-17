package com.mennangok1.reserved.roleAction;

import com.mennangok1.reserved.action.Action;
import com.mennangok1.reserved.action.ActionRepository;
import com.mennangok1.reserved.role.Role;
import com.mennangok1.reserved.role.RoleRepository;
import com.mennangok1.reserved.user.User;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class RoleActionService {

    private final RoleActionRepository roleActionRepository;
    private final RoleRepository roleRepository;
    private final ActionRepository actionRepository;
    private final PermissionService permissionService;

    public RoleActionService(
            RoleActionRepository roleActionRepository,
            RoleRepository roleRepository,
            ActionRepository actionRepository,
            PermissionService permissionService
    ) {
        this.roleActionRepository = roleActionRepository;
        this.roleRepository = roleRepository;
        this.actionRepository = actionRepository;
        this.permissionService = permissionService;
    }

    public List<RoleActionResponse> listGrants(User currentUser, String roleName) {
        permissionService.requirePermission(currentUser, "ROLE_ACTION_READ");

        List<RoleAction> grants = (roleName == null || roleName.isBlank())
                ? roleActionRepository.findAllWithRoleAndAction()
                : roleActionRepository.findByRole_NameWithAction(roleName);

        return grants.stream().map(RoleActionResponse::from).toList();
    }

    @Transactional
    public RoleActionResponse grant(User currentUser, RoleActionGrantRequest request) {
        permissionService.requirePermission(currentUser, "ROLE_ACTION_CREATE");

        Role role = resolveRole(request.roleName());
        Action action = resolveAction(request.actionName());

        if (roleActionRepository.existsByRole_IdAndAction_Name(role.getId(), action.getName())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This role already has this permission");
        }

        RoleAction roleAction = new RoleAction();
        roleAction.setRole(role);
        roleAction.setAction(action);
        RoleAction saved = roleActionRepository.save(roleAction);

        // Cross-bean call, so PermissionService's own @CacheEvict actually fires through its proxy.
        permissionService.evictPermissionCache(role.getId(), action.getName());

        return RoleActionResponse.from(saved);
    }

    @Transactional
    public void revoke(User currentUser, String roleName, String actionName) {
        permissionService.requirePermission(currentUser, "ROLE_ACTION_DELETE");

        Role role = resolveRole(roleName);
        Action action = resolveAction(actionName);

        RoleAction roleAction = roleActionRepository.findByRole_IdAndAction_Name(role.getId(), action.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "This role does not have this permission"));

        roleActionRepository.delete(roleAction);
        permissionService.evictPermissionCache(role.getId(), action.getName());
    }

    private Role resolveRole(String roleName) {
        return roleRepository.findByName(roleName)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown role: " + roleName));
    }

    private Action resolveAction(String actionName) {
        return actionRepository.findByName(actionName)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown action: " + actionName));
    }

}
