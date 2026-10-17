package com.mennangok1.reserved.roleAction;

import com.mennangok1.reserved.action.Action;
import com.mennangok1.reserved.action.ActionRepository;
import com.mennangok1.reserved.role.Role;
import com.mennangok1.reserved.role.RoleRepository;
import com.mennangok1.reserved.user.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoleActionServiceTest {

    @Mock
    private RoleActionRepository roleActionRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private ActionRepository actionRepository;
    @Mock
    private PermissionService permissionService;

    @InjectMocks
    private RoleActionService roleActionService;

    private User admin() {
        User user = new User();
        user.setId(1L);
        return user;
    }

    private Role role(Long id, String name) {
        Role role = new Role();
        role.setId(id);
        role.setName(name);
        return role;
    }

    private Action action(Long id, String name) {
        Action action = new Action();
        action.setId(id);
        action.setName(name);
        return action;
    }

    @Test
    void grant_savesNewRoleAction_andEvictsCache() {
        User admin = admin();
        Role customerRole = role(3L, "CUSTOMER");
        Action action = action(10L, "RESTAURANT_TABLE_READ");
        RoleActionGrantRequest request = new RoleActionGrantRequest("CUSTOMER", "RESTAURANT_TABLE_READ");

        when(roleRepository.findByName("CUSTOMER")).thenReturn(Optional.of(customerRole));
        when(actionRepository.findByName("RESTAURANT_TABLE_READ")).thenReturn(Optional.of(action));
        when(roleActionRepository.existsByRole_IdAndAction_Name(3L, "RESTAURANT_TABLE_READ")).thenReturn(false);
        when(roleActionRepository.save(org.mockito.ArgumentMatchers.any(RoleAction.class)))
                .thenAnswer(invocation -> {
                    RoleAction saved = invocation.getArgument(0);
                    saved.setId(99L);
                    return saved;
                });

        RoleActionResponse response = roleActionService.grant(admin, request);

        assertThat(response.id()).isEqualTo(99L);
        assertThat(response.roleName()).isEqualTo("CUSTOMER");
        assertThat(response.actionName()).isEqualTo("RESTAURANT_TABLE_READ");
        verify(permissionService).requirePermission(admin, "ROLE_ACTION_CREATE");
        verify(permissionService).evictPermissionCache(3L, "RESTAURANT_TABLE_READ");
    }

    @Test
    void grant_throwsConflict_whenAlreadyGranted() {
        User admin = admin();
        Role customerRole = role(3L, "CUSTOMER");
        Action action = action(10L, "RESTAURANT_TABLE_READ");
        RoleActionGrantRequest request = new RoleActionGrantRequest("CUSTOMER", "RESTAURANT_TABLE_READ");

        when(roleRepository.findByName("CUSTOMER")).thenReturn(Optional.of(customerRole));
        when(actionRepository.findByName("RESTAURANT_TABLE_READ")).thenReturn(Optional.of(action));
        when(roleActionRepository.existsByRole_IdAndAction_Name(3L, "RESTAURANT_TABLE_READ")).thenReturn(true);

        assertThatThrownBy(() -> roleActionService.grant(admin, request))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode().value())
                .isEqualTo(409);

        verify(roleActionRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void grant_throwsBadRequest_whenRoleUnknown() {
        User admin = admin();
        RoleActionGrantRequest request = new RoleActionGrantRequest("UNKNOWN", "RESTAURANT_TABLE_READ");

        when(roleRepository.findByName("UNKNOWN")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roleActionService.grant(admin, request))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode().value())
                .isEqualTo(400);
    }

    @Test
    void revoke_deletesRoleAction_andEvictsCache() {
        User admin = admin();
        Role customerRole = role(3L, "CUSTOMER");
        Action action = action(10L, "RESTAURANT_TABLE_READ");
        RoleAction existing = new RoleAction();
        existing.setId(5L);
        existing.setRole(customerRole);
        existing.setAction(action);

        when(roleRepository.findByName("CUSTOMER")).thenReturn(Optional.of(customerRole));
        when(actionRepository.findByName("RESTAURANT_TABLE_READ")).thenReturn(Optional.of(action));
        when(roleActionRepository.findByRole_IdAndAction_Name(3L, "RESTAURANT_TABLE_READ"))
                .thenReturn(Optional.of(existing));

        roleActionService.revoke(admin, "CUSTOMER", "RESTAURANT_TABLE_READ");

        verify(roleActionRepository).delete(existing);
        verify(permissionService).evictPermissionCache(3L, "RESTAURANT_TABLE_READ");
    }

    @Test
    void revoke_throwsNotFound_whenNotGranted() {
        User admin = admin();
        Role customerRole = role(3L, "CUSTOMER");
        Action action = action(10L, "RESTAURANT_TABLE_READ");

        when(roleRepository.findByName("CUSTOMER")).thenReturn(Optional.of(customerRole));
        when(actionRepository.findByName("RESTAURANT_TABLE_READ")).thenReturn(Optional.of(action));
        when(roleActionRepository.findByRole_IdAndAction_Name(3L, "RESTAURANT_TABLE_READ"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> roleActionService.revoke(admin, "CUSTOMER", "RESTAURANT_TABLE_READ"))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode().value())
                .isEqualTo(404);

        verify(roleActionRepository, never()).delete(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void listGrants_returnsAllGrants_whenRoleNameNotProvided() {
        User admin = admin();
        Role customerRole = role(3L, "CUSTOMER");
        Action action = action(10L, "RESTAURANT_TABLE_READ");
        RoleAction grant = new RoleAction();
        grant.setId(5L);
        grant.setRole(customerRole);
        grant.setAction(action);

        when(roleActionRepository.findAllWithRoleAndAction()).thenReturn(List.of(grant));

        List<RoleActionResponse> response = roleActionService.listGrants(admin, null);

        assertThat(response).hasSize(1);
        assertThat(response.get(0).roleName()).isEqualTo("CUSTOMER");
    }

}
