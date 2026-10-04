package com.mennangok1.reserved.itemType;

public record ItemTypeResponse(
        Long id,
        String name
) {

    static ItemTypeResponse from(ItemType itemType) {
        return new ItemTypeResponse(itemType.getId(), itemType.getName());
    }

}
