package com.mennangok1.reserved.action;

import com.mennangok1.reserved.roleAction.PermissionService;
import com.mennangok1.reserved.user.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.FORBIDDEN;

@ExtendWith(MockitoExtension.class)
class ActionServiceTest {

    @Mock
    private ActionRepository actionRepository;
    @Mock
    private PermissionService permissionService;

    @InjectMocks
    private ActionService actionService;

    private Action action(Long id, String name) {
        Action action = new Action();
        action.setId(id);
        action.setName(name);
        action.setDescription("desc");
        return action;
    }

    @Test
    void listActions_returnsAllActions() {
        User caller = new User();
        caller.setId(1L);

        when(actionRepository.findAll()).thenReturn(List.of(action(1L, "USER_READ")));

        List<ActionResponse> response = actionService.listActions(caller);

        assertThat(response).hasSize(1);
        assertThat(response.get(0).name()).isEqualTo("USER_READ");
        verify(permissionService).requirePermission(caller, "ROLE_ACTION_READ");
    }

    @Test
    void listActions_throwsForbidden_whenPermissionMissing() {
        User caller = new User();
        caller.setId(1L);

        doThrow(new ResponseStatusException(FORBIDDEN)).when(permissionService)
                .requirePermission(caller, "ROLE_ACTION_READ");

        assertThatThrownBy(() -> actionService.listActions(caller))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode().value())
                .isEqualTo(403);
    }

}
