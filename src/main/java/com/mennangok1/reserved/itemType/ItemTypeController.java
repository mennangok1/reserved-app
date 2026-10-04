package com.mennangok1.reserved.itemType;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/item-types")
class ItemTypeController {

    private final ItemTypeService itemTypeService;

    ItemTypeController(ItemTypeService itemTypeService) {
        this.itemTypeService = itemTypeService;
    }

    @GetMapping
    ResponseEntity<List<ItemTypeResponse>> listAll() {
        return ResponseEntity.ok(itemTypeService.listAll());
    }

}
