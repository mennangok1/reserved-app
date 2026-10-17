package com.mennangok1.reserved.roleAction;

import com.mennangok1.reserved.user.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/role-actions")
class RoleActionController {

    private final RoleActionService roleActionService;

    RoleActionController(RoleActionService roleActionService) {
        this.roleActionService = roleActionService;
    }

    @GetMapping
    ResponseEntity<List<RoleActionResponse>> listGrants(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String role
    ) {
        return ResponseEntity.ok(roleActionService.listGrants(principal.getUser(), role));
    }

    @PostMapping
    ResponseEntity<RoleActionResponse> grant(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody RoleActionGrantRequest request
    ) {
        RoleActionResponse response = roleActionService.grant(principal.getUser(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/{roleName}/{actionName}")
    ResponseEntity<Void> revoke(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String roleName,
            @PathVariable String actionName
    ) {
        roleActionService.revoke(principal.getUser(), roleName, actionName);
        return ResponseEntity.noContent().build();
    }

}
