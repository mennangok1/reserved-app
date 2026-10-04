package com.mennangok1.reserved.menuItem;

public record MenuItemResponse(
        Long id,
        String name,
        String description,
        Double price,
        Long menuOrder,
        Long itemTypeId,
        Long restaurantId
) {

    static MenuItemResponse from(MenuItem menuItem) {
        return new MenuItemResponse(
                menuItem.getId(),
                menuItem.getName(),
                menuItem.getDescription(),
                menuItem.getPrice(),
                menuItem.getMenuOrder(),
                menuItem.getItemType().getId(),
                menuItem.getRestaurant().getId()
        );
    }

}
