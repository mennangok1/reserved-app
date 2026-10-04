package com.mennangok1.reserved.menuItem;

import com.mennangok1.reserved.itemType.ItemType;
import com.mennangok1.reserved.itemType.ItemTypeRepository;
import com.mennangok1.reserved.restaurant.Restaurant;
import com.mennangok1.reserved.restaurantUser.RestaurantUser;
import com.mennangok1.reserved.restaurantUser.RestaurantUserRepository;
import com.mennangok1.reserved.roleAction.PermissionService;
import com.mennangok1.reserved.user.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.FORBIDDEN;

@ExtendWith(MockitoExtension.class)
class MenuItemServiceTest {

    @Mock
    private MenuItemRepository menuItemRepository;
    @Mock
    private ItemTypeRepository itemTypeRepository;
    @Mock
    private RestaurantUserRepository restaurantUserRepository;
    @Mock
    private PermissionService permissionService;

    @InjectMocks
    private MenuItemService menuItemService;

    private User user(Long id) {
        User user = new User();
        user.setId(id);
        return user;
    }

    private Restaurant restaurant(Long id) {
        Restaurant restaurant = new Restaurant();
        restaurant.setId(id);
        return restaurant;
    }

    private RestaurantUser linkFor(User user, Restaurant restaurant) {
        RestaurantUser link = new RestaurantUser();
        link.setUser(user);
        link.setRestaurant(restaurant);
        return link;
    }

    private ItemType itemType(Long id) {
        ItemType itemType = new ItemType();
        itemType.setId(id);
        itemType.setName("MAIN_COURSE");
        return itemType;
    }

    private MenuItem menuItem(Long id, Restaurant restaurant, ItemType itemType) {
        MenuItem menuItem = new MenuItem();
        menuItem.setId(id);
        menuItem.setName("Soup");
        menuItem.setPrice(9.5);
        menuItem.setMenuOrder(1L);
        menuItem.setRestaurant(restaurant);
        menuItem.setItemType(itemType);
        return menuItem;
    }

    @Test
    void create_throwsForbidden_whenPermissionMissing() {
        User customer = user(1L);
        MenuItemRequest request = new MenuItemRequest("Soup", null, 9.5, 1L, 2L);

        doThrow(new ResponseStatusException(FORBIDDEN)).when(permissionService)
                .requirePermission(customer, "MENU_ITEM_CREATE");

        assertThatThrownBy(() -> menuItemService.create(customer, request))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode().value())
                .isEqualTo(403);

        verify(restaurantUserRepository, never()).findByUser_Id(any());
        verify(menuItemRepository, never()).save(any());
    }

    @Test
    void create_throwsNotFound_whenCallerHasNoRestaurant() {
        User restaurantUser = user(1L);
        MenuItemRequest request = new MenuItemRequest("Soup", null, 9.5, 1L, 2L);

        when(restaurantUserRepository.findByUser_Id(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> menuItemService.create(restaurantUser, request))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode().value())
                .isEqualTo(404);
    }

    @Test
    void create_throwsBadRequest_whenItemTypeUnknown() {
        User restaurantUser = user(1L);
        Restaurant ownRestaurant = restaurant(10L);
        MenuItemRequest request = new MenuItemRequest("Soup", null, 9.5, 1L, 99L);

        when(restaurantUserRepository.findByUser_Id(1L)).thenReturn(Optional.of(linkFor(restaurantUser, ownRestaurant)));
        when(itemTypeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> menuItemService.create(restaurantUser, request))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode().value())
                .isEqualTo(400);
    }

    @Test
    void create_savesMenuItemUnderCallersOwnRestaurant() {
        User restaurantUser = user(1L);
        Restaurant ownRestaurant = restaurant(10L);
        ItemType type = itemType(2L);
        MenuItemRequest request = new MenuItemRequest("Soup", "Tomato soup", 9.5, 1L, 2L);

        when(restaurantUserRepository.findByUser_Id(1L)).thenReturn(Optional.of(linkFor(restaurantUser, ownRestaurant)));
        when(itemTypeRepository.findById(2L)).thenReturn(Optional.of(type));
        when(menuItemRepository.save(any(MenuItem.class))).thenAnswer(invocation -> {
            MenuItem saved = invocation.getArgument(0);
            saved.setId(100L);
            return saved;
        });

        MenuItemResponse response = menuItemService.create(restaurantUser, request);

        assertThat(response.id()).isEqualTo(100L);
        assertThat(response.restaurantId()).isEqualTo(10L);
        assertThat(response.itemTypeId()).isEqualTo(2L);
    }

    @Test
    void listOwn_returnsOnlyCallersOwnRestaurantItems() {
        User restaurantUser = user(1L);
        Restaurant ownRestaurant = restaurant(10L);
        ItemType type = itemType(2L);

        when(restaurantUserRepository.findByUser_Id(1L)).thenReturn(Optional.of(linkFor(restaurantUser, ownRestaurant)));
        when(menuItemRepository.findByRestaurant_Id(10L))
                .thenReturn(List.of(menuItem(100L, ownRestaurant, type)));

        List<MenuItemResponse> response = menuItemService.listOwn(restaurantUser);

        assertThat(response).hasSize(1);
        assertThat(response.get(0).restaurantId()).isEqualTo(10L);
    }

    @Test
    void update_throwsNotFound_whenMenuItemBelongsToAnotherRestaurant() {
        User restaurantUserB = user(2L);
        Restaurant restaurantB = restaurant(20L);
        Restaurant restaurantA = restaurant(10L);
        ItemType type = itemType(2L);
        MenuItem itemOwnedByA = menuItem(100L, restaurantA, type);
        MenuItemRequest request = new MenuItemRequest("Soup", null, 9.5, 1L, 2L);

        when(restaurantUserRepository.findByUser_Id(2L)).thenReturn(Optional.of(linkFor(restaurantUserB, restaurantB)));
        when(menuItemRepository.findById(100L)).thenReturn(Optional.of(itemOwnedByA));

        assertThatThrownBy(() -> menuItemService.update(restaurantUserB, 100L, request))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode().value())
                .isEqualTo(404);

        verify(menuItemRepository, never()).save(any());
    }

    @Test
    void update_updatesMenuItem_whenCallerOwnsIt() {
        User restaurantUser = user(1L);
        Restaurant ownRestaurant = restaurant(10L);
        ItemType originalType = itemType(2L);
        ItemType newType = itemType(3L);
        MenuItem existing = menuItem(100L, ownRestaurant, originalType);
        MenuItemRequest request = new MenuItemRequest("Updated Soup", "Now spicy", 11.0, 2L, 3L);

        when(restaurantUserRepository.findByUser_Id(1L)).thenReturn(Optional.of(linkFor(restaurantUser, ownRestaurant)));
        when(menuItemRepository.findById(100L)).thenReturn(Optional.of(existing));
        when(itemTypeRepository.findById(3L)).thenReturn(Optional.of(newType));
        when(menuItemRepository.save(any(MenuItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MenuItemResponse response = menuItemService.update(restaurantUser, 100L, request);

        assertThat(response.name()).isEqualTo("Updated Soup");
        assertThat(response.price()).isEqualTo(11.0);
        assertThat(response.itemTypeId()).isEqualTo(3L);
    }

    @Test
    void delete_throwsNotFound_whenMenuItemBelongsToAnotherRestaurant() {
        User restaurantUserB = user(2L);
        Restaurant restaurantB = restaurant(20L);
        Restaurant restaurantA = restaurant(10L);
        ItemType type = itemType(2L);
        MenuItem itemOwnedByA = menuItem(100L, restaurantA, type);

        when(restaurantUserRepository.findByUser_Id(2L)).thenReturn(Optional.of(linkFor(restaurantUserB, restaurantB)));
        when(menuItemRepository.findById(100L)).thenReturn(Optional.of(itemOwnedByA));

        assertThatThrownBy(() -> menuItemService.delete(restaurantUserB, 100L))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode().value())
                .isEqualTo(404);

        verify(menuItemRepository, never()).delete(any());
    }

    @Test
    void delete_deletesMenuItem_whenCallerOwnsIt() {
        User restaurantUser = user(1L);
        Restaurant ownRestaurant = restaurant(10L);
        ItemType type = itemType(2L);
        MenuItem existing = menuItem(100L, ownRestaurant, type);

        when(restaurantUserRepository.findByUser_Id(1L)).thenReturn(Optional.of(linkFor(restaurantUser, ownRestaurant)));
        when(menuItemRepository.findById(100L)).thenReturn(Optional.of(existing));

        menuItemService.delete(restaurantUser, 100L);

        verify(menuItemRepository).delete(existing);
    }

}
