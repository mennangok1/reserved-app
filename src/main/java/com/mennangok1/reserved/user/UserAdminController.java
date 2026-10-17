package com.mennangok1.reserved.user;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/users")
class UserAdminController {

    private final UserService userService;

    UserAdminController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    ResponseEntity<List<UserResponse>> listAll(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(userService.listAllUsers(principal.getUser()));
    }

}
