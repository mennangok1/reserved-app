package com.mennangok1.reserved.menuItem;

import com.mennangok1.reserved.itemType.ItemType;
import com.mennangok1.reserved.itemType.ItemTypeRepository;
import com.mennangok1.reserved.restaurant.Restaurant;
import com.mennangok1.reserved.restaurant.RestaurantRepository;
import com.mennangok1.reserved.restaurantUser.RestaurantUserRepository;
import com.mennangok1.reserved.roleAction.PermissionService;
import com.mennangok1.reserved.user.User;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class MenuItemService {

    private final MenuItemRepository menuItemRepository;
    private final ItemTypeRepository itemTypeRepository;
    private final RestaurantUserRepository restaurantUserRepository;
    private final RestaurantRepository restaurantRepository;
    private final PermissionService permissionService;

    public MenuItemService(
            MenuItemRepository menuItemRepository,
            ItemTypeRepository itemTypeRepository,
            RestaurantUserRepository restaurantUserRepository,
            RestaurantRepository restaurantRepository,
            PermissionService permissionService
    ) {
        this.menuItemRepository = menuItemRepository;
        this.itemTypeRepository = itemTypeRepository;
        this.restaurantUserRepository = restaurantUserRepository;
        this.restaurantRepository = restaurantRepository;
        this.permissionService = permissionService;
    }

    @Transactional
    public MenuItemResponse create(User currentUser, MenuItemRequest request) {
        permissionService.requirePermission(currentUser, "MENU_ITEM_CREATE");

        Restaurant ownRestaurant = resolveOwnRestaurant(currentUser);
        ItemType itemType = resolveItemType(request.itemTypeId());

        MenuItem menuItem = new MenuItem();
        menuItem.setName(request.name());
        menuItem.setDescription(request.description());
        menuItem.setPrice(request.price());
        menuItem.setMenuOrder(request.menuOrder());
        menuItem.setItemType(itemType);
        menuItem.setRestaurant(ownRestaurant);

        MenuItem saved = menuItemRepository.save(menuItem);
        return MenuItemResponse.from(saved);
    }

    public List<MenuItemResponse> listOwn(User currentUser) {
        permissionService.requirePermission(currentUser, "MENU_ITEM_READ");

        Restaurant ownRestaurant = resolveOwnRestaurant(currentUser);
        return menuItemRepository.findByRestaurant_Id(ownRestaurant.getId()).stream()
                .map(MenuItemResponse::from)
                .toList();
    }

    @Transactional
    public MenuItemResponse update(User currentUser, Long menuItemId, MenuItemRequest request) {
        permissionService.requirePermission(currentUser, "MENU_ITEM_UPDATE");

        Restaurant ownRestaurant = resolveOwnRestaurant(currentUser);
        MenuItem menuItem = resolveOwnedMenuItem(menuItemId, ownRestaurant.getId());
        ItemType itemType = resolveItemType(request.itemTypeId());

        menuItem.setName(request.name());
        menuItem.setDescription(request.description());
        menuItem.setPrice(request.price());
        menuItem.setMenuOrder(request.menuOrder());
        menuItem.setItemType(itemType);

        MenuItem saved = menuItemRepository.save(menuItem);
        return MenuItemResponse.from(saved);
    }

    @Transactional
    public void delete(User currentUser, Long menuItemId) {
        permissionService.requirePermission(currentUser, "MENU_ITEM_DELETE");

        Restaurant ownRestaurant = resolveOwnRestaurant(currentUser);
        MenuItem menuItem = resolveOwnedMenuItem(menuItemId, ownRestaurant.getId());

        menuItemRepository.delete(menuItem);
    }

    public List<MenuItemResponse> listByRestaurant(User currentUser, Long restaurantId) {
        permissionService.requirePermission(currentUser, "RESTAURANT_READ");

        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Restaurant not found"));

        return menuItemRepository.findByRestaurant_Id(restaurant.getId()).stream()
                .map(MenuItemResponse::from)
                .toList();
    }

    private Restaurant resolveOwnRestaurant(User currentUser) {
        return restaurantUserRepository.findByUser_Id(currentUser.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "You don't manage a restaurant yet"))
                .getRestaurant();
    }

    private MenuItem resolveOwnedMenuItem(Long menuItemId, Long ownRestaurantId) {
        MenuItem menuItem = menuItemRepository.findById(menuItemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Menu item not found"));

        // 404 rather than 403 on mismatch: don't confirm another restaurant's menu item even exists.
        if (!menuItem.getRestaurant().getId().equals(ownRestaurantId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Menu item not found");
        }

        return menuItem;
    }

    private ItemType resolveItemType(Long itemTypeId) {
        return itemTypeRepository.findById(itemTypeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown item type"));
    }

}
