package com.mennangok1.reserved.roleAction;

import com.mennangok1.reserved.role.Role;
import com.mennangok1.reserved.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PermissionServiceTest {

    @Mock
    private RoleActionRepository roleActionRepository;

    private PermissionService permissionService;

    // No Spring proxy in a plain Mockito unit test, so requirePermission's self-invocation of
    // hasPermission needs a manually wired "self" — wiring it to the same instance is correct
    // here since only the business logic (not the @Cacheable behavior) is under test.
    @BeforeEach
    void setUp() {
        permissionService = new PermissionService(roleActionRepository, null);
        ReflectionTestUtils.setField(permissionService, "self", permissionService);
    }

    @Test
    void hasPermission_returnsTrue_whenRoleActionExists() {
        when(roleActionRepository.existsByRole_IdAndAction_Name(1L, "MENU_ITEM_CREATE")).thenReturn(true);

        assertThat(permissionService.hasPermission(1L, "MENU_ITEM_CREATE")).isTrue();
    }

    @Test
    void hasPermission_returnsFalse_whenRoleActionMissing() {
        when(roleActionRepository.existsByRole_IdAndAction_Name(1L, "MENU_ITEM_CREATE")).thenReturn(false);

        assertThat(permissionService.hasPermission(1L, "MENU_ITEM_CREATE")).isFalse();
    }

    @Test
    void requirePermission_throwsForbidden_whenRoleLacksAction() {
        when(roleActionRepository.existsByRole_IdAndAction_Name(3L, "MENU_ITEM_CREATE")).thenReturn(false);

        User customer = userWithRole(3L, "CUSTOMER");

        assertThatThrownBy(() -> permissionService.requirePermission(customer, "MENU_ITEM_CREATE"))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode().value())
                .isEqualTo(403);
    }

    @Test
    void requirePermission_doesNotThrow_whenRoleHasAction() {
        when(roleActionRepository.existsByRole_IdAndAction_Name(2L, "MENU_ITEM_CREATE")).thenReturn(true);

        User restaurantUser = userWithRole(2L, "RESTAURANT_USER");

        permissionService.requirePermission(restaurantUser, "MENU_ITEM_CREATE");
    }

    private User userWithRole(Long roleId, String roleName) {
        Role role = new Role();
        role.setId(roleId);
        role.setName(roleName);

        User user = new User();
        user.setRole(role);
        return user;
    }

}
