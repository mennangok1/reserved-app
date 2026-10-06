package com.mennangok1.reserved.integration;

import com.mennangok1.reserved.menuItem.MenuItemRequest;
import com.mennangok1.reserved.restaurant.CreateRestaurantRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// CUSTOMER slice (ADR-003 next step): restaurant search + read-only menu viewing.
class CustomerBrowseFlowTest extends AbstractIntegrationTest {

    private String createRestaurantUserWithRestaurant(String name) throws Exception {
        String token = registerRestaurantUserAndLogin(uniqueEmail("restaurant-user"));
        mockMvc.perform(post("/restaurants")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateRestaurantRequest(name, null))))
                .andExpect(status().isCreated());
        return token;
    }

    private Long ownRestaurantId(String token) throws Exception {
        MvcResult result = mockMvc.perform(get("/restaurants/mine").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private void createMenuItem(String token, String name, Long itemTypeId) throws Exception {
        MenuItemRequest request = new MenuItemRequest(name, "desc", 9.5, 1L, itemTypeId);
        mockMvc.perform(post("/menu-items")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    void listRestaurants_returns200_andFiltersByName_forCustomer() throws Exception {
        createRestaurantUserWithRestaurant("Chez Jane");
        createRestaurantUserWithRestaurant("Totally Different Place");
        String customerToken = registerCustomerAndLogin(uniqueEmail("customer"));

        mockMvc.perform(get("/restaurants").param("name", "Chez").header("Authorization", bearer(customerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Chez Jane"));
    }

    @Test
    void getRestaurant_returns200_forCustomer() throws Exception {
        String ownerToken = createRestaurantUserWithRestaurant("Chez Jane");
        Long restaurantId = ownRestaurantId(ownerToken);
        String customerToken = registerCustomerAndLogin(uniqueEmail("customer"));

        mockMvc.perform(get("/restaurants/" + restaurantId).header("Authorization", bearer(customerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Chez Jane"));
    }

    @Test
    void getRestaurant_returns404_whenMissing() throws Exception {
        String customerToken = registerCustomerAndLogin(uniqueEmail("customer"));

        mockMvc.perform(get("/restaurants/999999").header("Authorization", bearer(customerToken)))
                .andExpect(status().isNotFound());
    }

    @Test
    void listMenuItems_returns200_withAnotherRestaurantsMenu_forCustomer() throws Exception {
        String ownerToken = createRestaurantUserWithRestaurant("Chez Jane");
        Long restaurantId = ownRestaurantId(ownerToken);
        Long itemTypeId = firstItemTypeId(ownerToken);
        createMenuItem(ownerToken, "Soup", itemTypeId);

        String customerToken = registerCustomerAndLogin(uniqueEmail("customer"));

        mockMvc.perform(get("/restaurants/" + restaurantId + "/menu-items").header("Authorization", bearer(customerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Soup"));
    }

    @Test
    void listRestaurants_returns403_forRestaurantUser() throws Exception {
        String restaurantUserToken = createRestaurantUserWithRestaurant("Chez Jane");

        mockMvc.perform(get("/restaurants").header("Authorization", bearer(restaurantUserToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    void listMenuItems_returns403_whenRestaurantUserTriesAnotherRestaurant() throws Exception {
        String ownerAToken = createRestaurantUserWithRestaurant("Restaurant A");
        Long restaurantAId = ownRestaurantId(ownerAToken);
        String ownerBToken = createRestaurantUserWithRestaurant("Restaurant B");

        // RESTAURANT_USER B has MENU_ITEM_READ for its own CRUD, but not RESTAURANT_READ,
        // so browsing restaurant A's menu through this endpoint must still be forbidden.
        mockMvc.perform(get("/restaurants/" + restaurantAId + "/menu-items").header("Authorization", bearer(ownerBToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    void listRestaurants_returns401_whenNoToken() throws Exception {
        mockMvc.perform(get("/restaurants"))
                .andExpect(status().isUnauthorized());
    }

}
