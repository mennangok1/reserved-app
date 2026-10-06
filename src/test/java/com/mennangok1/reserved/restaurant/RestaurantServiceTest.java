package com.mennangok1.reserved.restaurant;

import com.mennangok1.reserved.restaurantUser.RestaurantUser;
import com.mennangok1.reserved.restaurantUser.RestaurantUserRepository;
import com.mennangok1.reserved.role.Role;
import com.mennangok1.reserved.roleAction.PermissionService;
import com.mennangok1.reserved.user.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
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

@ExtendWith(MockitoExtension.class)
class RestaurantServiceTest {

    @Mock
    private RestaurantRepository restaurantRepository;
    @Mock
    private RestaurantUserRepository restaurantUserRepository;
    @Mock
    private PermissionService permissionService;

    @InjectMocks
    private RestaurantService restaurantService;

    private User userWithRole(Long userId, String roleName) {
        Role role = new Role();
        role.setName(roleName);

        User user = new User();
        user.setId(userId);
        user.setRole(role);
        return user;
    }

    @Test
    void createOwnRestaurant_savesRestaurantAndLink_whenRestaurantUserHasNoneYet() {
        User user = userWithRole(1L, "RESTAURANT_USER");
        CreateRestaurantRequest request = new CreateRestaurantRequest("Chez Jane", "French bistro");

        when(restaurantUserRepository.existsByUser_Id(1L)).thenReturn(false);
        when(restaurantRepository.save(any(Restaurant.class))).thenAnswer(invocation -> {
            Restaurant restaurant = invocation.getArgument(0);
            restaurant.setId(10L);
            return restaurant;
        });

        RestaurantResponse response = restaurantService.createOwnRestaurant(user, request);

        ArgumentCaptor<RestaurantUser> linkCaptor = ArgumentCaptor.forClass(RestaurantUser.class);
        verify(restaurantUserRepository).save(linkCaptor.capture());

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.name()).isEqualTo("Chez Jane");
        assertThat(linkCaptor.getValue().getUser()).isEqualTo(user);
        assertThat(linkCaptor.getValue().getRestaurant().getId()).isEqualTo(10L);
    }

    @Test
    void createOwnRestaurant_throwsForbidden_whenUserIsNotRestaurantUser() {
        User customer = userWithRole(1L, "CUSTOMER");
        CreateRestaurantRequest request = new CreateRestaurantRequest("Chez Jane", null);

        assertThatThrownBy(() -> restaurantService.createOwnRestaurant(customer, request))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode().value())
                .isEqualTo(403);

        verify(restaurantRepository, never()).save(any());
    }

    @Test
    void createOwnRestaurant_throwsConflict_whenUserAlreadyManagesARestaurant() {
        User user = userWithRole(1L, "RESTAURANT_USER");
        CreateRestaurantRequest request = new CreateRestaurantRequest("Chez Jane", null);

        when(restaurantUserRepository.existsByUser_Id(1L)).thenReturn(true);

        assertThatThrownBy(() -> restaurantService.createOwnRestaurant(user, request))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode().value())
                .isEqualTo(409);

        verify(restaurantRepository, never()).save(any());
    }

    @Test
    void getOwnRestaurant_returnsRestaurant_whenLinkExists() {
        User user = userWithRole(1L, "RESTAURANT_USER");
        Restaurant restaurant = new Restaurant();
        restaurant.setId(10L);
        restaurant.setName("Chez Jane");

        RestaurantUser link = new RestaurantUser();
        link.setUser(user);
        link.setRestaurant(restaurant);

        when(restaurantUserRepository.findByUser_Id(1L)).thenReturn(Optional.of(link));

        RestaurantResponse response = restaurantService.getOwnRestaurant(user);

        assertThat(response.id()).isEqualTo(10L);
    }

    @Test
    void getOwnRestaurant_throwsNotFound_whenNoRestaurantYet() {
        User user = userWithRole(1L, "RESTAURANT_USER");

        when(restaurantUserRepository.findByUser_Id(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> restaurantService.getOwnRestaurant(user))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode().value())
                .isEqualTo(404);
    }

    @Test
    void getOwnRestaurant_throwsForbidden_whenUserIsNotRestaurantUser() {
        User admin = userWithRole(1L, "ADMIN");

        assertThatThrownBy(() -> restaurantService.getOwnRestaurant(admin))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode().value())
                .isEqualTo(403);
    }

    @Test
    void listRestaurants_returnsAll_whenNoNameFilterGiven() {
        User customer = userWithRole(1L, "CUSTOMER");
        Restaurant restaurantA = new Restaurant();
        restaurantA.setId(10L);
        restaurantA.setName("Chez Jane");

        when(restaurantRepository.findAll()).thenReturn(List.of(restaurantA));

        List<RestaurantResponse> response = restaurantService.listRestaurants(customer, null);

        assertThat(response).hasSize(1);
        assertThat(response.get(0).id()).isEqualTo(10L);
        verify(restaurantRepository, never()).findByNameContainingIgnoreCase(any());
    }

    @Test
    void listRestaurants_filtersByName_whenNameGiven() {
        User customer = userWithRole(1L, "CUSTOMER");
        Restaurant restaurantA = new Restaurant();
        restaurantA.setId(10L);
        restaurantA.setName("Chez Jane");

        when(restaurantRepository.findByNameContainingIgnoreCase("jane")).thenReturn(List.of(restaurantA));

        List<RestaurantResponse> response = restaurantService.listRestaurants(customer, "jane");

        assertThat(response).hasSize(1);
        verify(restaurantRepository, never()).findAll();
    }

    @Test
    void listRestaurants_throwsForbidden_whenPermissionMissing() {
        User restaurantUser = userWithRole(1L, "RESTAURANT_USER");

        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN)).when(permissionService)
                .requirePermission(restaurantUser, "RESTAURANT_READ");

        assertThatThrownBy(() -> restaurantService.listRestaurants(restaurantUser, null))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode().value())
                .isEqualTo(403);

        verify(restaurantRepository, never()).findAll();
    }

    @Test
    void getRestaurant_returnsRestaurant_whenFound() {
        User customer = userWithRole(1L, "CUSTOMER");
        Restaurant restaurant = new Restaurant();
        restaurant.setId(10L);
        restaurant.setName("Chez Jane");

        when(restaurantRepository.findById(10L)).thenReturn(Optional.of(restaurant));

        RestaurantResponse response = restaurantService.getRestaurant(customer, 10L);

        assertThat(response.id()).isEqualTo(10L);
    }

    @Test
    void getRestaurant_throwsNotFound_whenMissing() {
        User customer = userWithRole(1L, "CUSTOMER");

        when(restaurantRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> restaurantService.getRestaurant(customer, 999L))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode().value())
                .isEqualTo(404);
    }

    @Test
    void getRestaurant_throwsForbidden_whenPermissionMissing() {
        User restaurantUser = userWithRole(1L, "RESTAURANT_USER");

        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN)).when(permissionService)
                .requirePermission(restaurantUser, "RESTAURANT_READ");

        assertThatThrownBy(() -> restaurantService.getRestaurant(restaurantUser, 10L))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode().value())
                .isEqualTo(403);

        verify(restaurantRepository, never()).findById(any());
    }

}
