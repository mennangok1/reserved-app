package com.mennangok1.reserved.restaurantTable;

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
class RestaurantTableServiceTest {

    @Mock
    private RestaurantTableRepository restaurantTableRepository;
    @Mock
    private RestaurantUserRepository restaurantUserRepository;
    @Mock
    private PermissionService permissionService;

    @InjectMocks
    private RestaurantTableService restaurantTableService;

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

    private RestaurantTable restaurantTable(Long id, Restaurant restaurant) {
        RestaurantTable restaurantTable = new RestaurantTable();
        restaurantTable.setId(id);
        restaurantTable.setLabel("T1");
        restaurantTable.setCapacity(4L);
        restaurantTable.setRestaurant(restaurant);
        return restaurantTable;
    }

    @Test
    void create_throwsForbidden_whenPermissionMissing() {
        User customer = user(1L);
        RestaurantTableRequest request = new RestaurantTableRequest("T1", 4L);

        doThrow(new ResponseStatusException(FORBIDDEN)).when(permissionService)
                .requirePermission(customer, "RESTAURANT_TABLE_CREATE");

        assertThatThrownBy(() -> restaurantTableService.create(customer, request))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode().value())
                .isEqualTo(403);

        verify(restaurantUserRepository, never()).findByUser_Id(any());
        verify(restaurantTableRepository, never()).save(any());
    }

    @Test
    void create_throwsNotFound_whenCallerHasNoRestaurant() {
        User restaurantUser = user(1L);
        RestaurantTableRequest request = new RestaurantTableRequest("T1", 4L);

        when(restaurantUserRepository.findByUser_Id(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> restaurantTableService.create(restaurantUser, request))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode().value())
                .isEqualTo(404);
    }

    @Test
    void create_savesTableUnderCallersOwnRestaurant() {
        User restaurantUser = user(1L);
        Restaurant ownRestaurant = restaurant(10L);
        RestaurantTableRequest request = new RestaurantTableRequest("T1", 4L);

        when(restaurantUserRepository.findByUser_Id(1L)).thenReturn(Optional.of(linkFor(restaurantUser, ownRestaurant)));
        when(restaurantTableRepository.save(any(RestaurantTable.class))).thenAnswer(invocation -> {
            RestaurantTable saved = invocation.getArgument(0);
            saved.setId(100L);
            return saved;
        });

        RestaurantTableResponse response = restaurantTableService.create(restaurantUser, request);

        assertThat(response.id()).isEqualTo(100L);
        assertThat(response.label()).isEqualTo("T1");
        assertThat(response.capacity()).isEqualTo(4L);
        assertThat(response.restaurantId()).isEqualTo(10L);
    }

    @Test
    void listOwn_returnsOnlyCallersOwnRestaurantTables() {
        User restaurantUser = user(1L);
        Restaurant ownRestaurant = restaurant(10L);

        when(restaurantUserRepository.findByUser_Id(1L)).thenReturn(Optional.of(linkFor(restaurantUser, ownRestaurant)));
        when(restaurantTableRepository.findByRestaurant_Id(10L))
                .thenReturn(List.of(restaurantTable(100L, ownRestaurant)));

        List<RestaurantTableResponse> response = restaurantTableService.listOwn(restaurantUser);

        assertThat(response).hasSize(1);
        assertThat(response.get(0).restaurantId()).isEqualTo(10L);
    }

    @Test
    void update_throwsNotFound_whenTableBelongsToAnotherRestaurant() {
        User restaurantUserB = user(2L);
        Restaurant restaurantB = restaurant(20L);
        Restaurant restaurantA = restaurant(10L);
        RestaurantTable tableOwnedByA = restaurantTable(100L, restaurantA);
        RestaurantTableRequest request = new RestaurantTableRequest("T1", 4L);

        when(restaurantUserRepository.findByUser_Id(2L)).thenReturn(Optional.of(linkFor(restaurantUserB, restaurantB)));
        when(restaurantTableRepository.findById(100L)).thenReturn(Optional.of(tableOwnedByA));

        assertThatThrownBy(() -> restaurantTableService.update(restaurantUserB, 100L, request))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode().value())
                .isEqualTo(404);

        verify(restaurantTableRepository, never()).save(any());
    }

    @Test
    void update_updatesTable_whenCallerOwnsIt() {
        User restaurantUser = user(1L);
        Restaurant ownRestaurant = restaurant(10L);
        RestaurantTable existing = restaurantTable(100L, ownRestaurant);
        RestaurantTableRequest request = new RestaurantTableRequest("T2", 6L);

        when(restaurantUserRepository.findByUser_Id(1L)).thenReturn(Optional.of(linkFor(restaurantUser, ownRestaurant)));
        when(restaurantTableRepository.findById(100L)).thenReturn(Optional.of(existing));
        when(restaurantTableRepository.save(any(RestaurantTable.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RestaurantTableResponse response = restaurantTableService.update(restaurantUser, 100L, request);

        assertThat(response.label()).isEqualTo("T2");
        assertThat(response.capacity()).isEqualTo(6L);
    }

    @Test
    void delete_throwsNotFound_whenTableBelongsToAnotherRestaurant() {
        User restaurantUserB = user(2L);
        Restaurant restaurantB = restaurant(20L);
        Restaurant restaurantA = restaurant(10L);
        RestaurantTable tableOwnedByA = restaurantTable(100L, restaurantA);

        when(restaurantUserRepository.findByUser_Id(2L)).thenReturn(Optional.of(linkFor(restaurantUserB, restaurantB)));
        when(restaurantTableRepository.findById(100L)).thenReturn(Optional.of(tableOwnedByA));

        assertThatThrownBy(() -> restaurantTableService.delete(restaurantUserB, 100L))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode().value())
                .isEqualTo(404);

        verify(restaurantTableRepository, never()).delete(any());
    }

    @Test
    void delete_deletesTable_whenCallerOwnsIt() {
        User restaurantUser = user(1L);
        Restaurant ownRestaurant = restaurant(10L);
        RestaurantTable existing = restaurantTable(100L, ownRestaurant);

        when(restaurantUserRepository.findByUser_Id(1L)).thenReturn(Optional.of(linkFor(restaurantUser, ownRestaurant)));
        when(restaurantTableRepository.findById(100L)).thenReturn(Optional.of(existing));

        restaurantTableService.delete(restaurantUser, 100L);

        verify(restaurantTableRepository).delete(existing);
    }

}
