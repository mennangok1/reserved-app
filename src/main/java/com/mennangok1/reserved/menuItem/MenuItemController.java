package com.mennangok1.reserved.menuItem;

import com.mennangok1.reserved.user.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/menu-items")
class MenuItemController {

    private final MenuItemService menuItemService;

    MenuItemController(MenuItemService menuItemService) {
        this.menuItemService = menuItemService;
    }

    @PostMapping
    ResponseEntity<MenuItemResponse> create(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody MenuItemRequest request
    ) {
        MenuItemResponse response = menuItemService.create(principal.getUser(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    ResponseEntity<List<MenuItemResponse>> listOwn(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(menuItemService.listOwn(principal.getUser()));
    }

    @PutMapping("/{id}")
    ResponseEntity<MenuItemResponse> update(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody MenuItemRequest request
    ) {
        MenuItemResponse response = menuItemService.update(principal.getUser(), id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long id) {
        menuItemService.delete(principal.getUser(), id);
        return ResponseEntity.noContent().build();
    }

}
