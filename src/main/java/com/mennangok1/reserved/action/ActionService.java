package com.mennangok1.reserved.action;

import com.mennangok1.reserved.roleAction.PermissionService;
import com.mennangok1.reserved.user.User;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ActionService {

    private final ActionRepository actionRepository;
    private final PermissionService permissionService;

    public ActionService(ActionRepository actionRepository, PermissionService permissionService) {
        this.actionRepository = actionRepository;
        this.permissionService = permissionService;
    }

    // The action catalog itself stays migration-managed (no POST here) — only the
    // role-to-action mapping is runtime-manageable, via RoleActionService.
    public List<ActionResponse> listActions(User currentUser) {
        permissionService.requirePermission(currentUser, "ROLE_ACTION_READ");
        return actionRepository.findAll().stream()
                .map(ActionResponse::from)
                .toList();
    }

}
