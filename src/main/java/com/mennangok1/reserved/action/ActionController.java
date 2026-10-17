package com.mennangok1.reserved.action;

import com.mennangok1.reserved.user.UserPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/actions")
class ActionController {

    private final ActionService actionService;

    ActionController(ActionService actionService) {
        this.actionService = actionService;
    }

    @GetMapping
    ResponseEntity<List<ActionResponse>> listActions(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(actionService.listActions(principal.getUser()));
    }

}
