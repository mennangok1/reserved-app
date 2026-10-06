package com.mennangok1.reserved.integration;

import com.mennangok1.reserved.menuItem.MenuItemRequest;
import com.mennangok1.reserved.restaurant.CreateRestaurantRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MenuItemFlowTest extends AbstractIntegrationTest {

    private String createRestaurantUserWithRestaurant(String name) throws Exception {
        String token = registerRestaurantUserAndLogin(uniqueEmail("restaurant-user"));
        mockMvc.perform(post("/restaurants")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateRestaurantRequest(name, null))))
                .andExpect(status().isCreated());
        return token;
    }

    private Long createMenuItem(String token, String name, Long itemTypeId) throws Exception {
        MenuItemRequest request = new MenuItemRequest(name, "desc", 9.5, 1L, itemTypeId);
        MvcResult result = mockMvc.perform(post("/menu-items")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    @Test
    void createMenuItem_returns201_forOwnRestaurant() throws Exception {
        String token = createRestaurantUserWithRestaurant("Chez Jane");
        Long itemTypeId = firstItemTypeId(token);

        MenuItemRequest request = new MenuItemRequest("Soup", "Tomato soup", 9.5, 1L, itemTypeId);

        mockMvc.perform(post("/menu-items")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Soup"));
    }

    @Test
    void createMenuItem_returns403_forCustomer() throws Exception {
        String customerToken = registerCustomerAndLogin(uniqueEmail("customer"));
        Long itemTypeId = firstItemTypeId(customerToken);

        MenuItemRequest request = new MenuItemRequest("Soup", null, 9.5, 1L, itemTypeId);

        mockMvc.perform(post("/menu-items")
                        .header("Authorization", bearer(customerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void createMenuItem_returns400_whenItemTypeUnknown() throws Exception {
        String token = createRestaurantUserWithRestaurant("Chez Jane");

        MenuItemRequest request = new MenuItemRequest("Soup", null, 9.5, 1L, 999999L);

        mockMvc.perform(post("/menu-items")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listOwn_returnsOnlyCallersOwnRestaurantItems() throws Exception {
        String tokenA = createRestaurantUserWithRestaurant("Restaurant A");
        String tokenB = createRestaurantUserWithRestaurant("Restaurant B");
        Long itemTypeId = firstItemTypeId(tokenA);

        createMenuItem(tokenA, "A's Soup", itemTypeId);
        createMenuItem(tokenB, "B's Soup", itemTypeId);

        mockMvc.perform(get("/menu-items").header("Authorization", bearer(tokenA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("A's Soup"));
    }

    @Test
    void updateMenuItem_returns200_whenCallerOwnsIt() throws Exception {
        String token = createRestaurantUserWithRestaurant("Chez Jane");
        Long itemTypeId = firstItemTypeId(token);
        Long menuItemId = createMenuItem(token, "Soup", itemTypeId);

        MenuItemRequest update = new MenuItemRequest("Updated Soup", "Now spicy", 11.0, 2L, itemTypeId);

        mockMvc.perform(put("/menu-items/" + menuItemId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Soup"));
    }

    @Test
    void updateMenuItem_returns404_whenBelongsToAnotherRestaurant() throws Exception {
        String tokenA = createRestaurantUserWithRestaurant("Restaurant A");
        String tokenB = createRestaurantUserWithRestaurant("Restaurant B");
        Long itemTypeId = firstItemTypeId(tokenA);
        Long menuItemId = createMenuItem(tokenA, "A's Soup", itemTypeId);

        MenuItemRequest update = new MenuItemRequest("Hijacked", null, 1.0, 1L, itemTypeId);

        mockMvc.perform(put("/menu-items/" + menuItemId)
                        .header("Authorization", bearer(tokenB))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteMenuItem_returns204_whenCallerOwnsIt() throws Exception {
        String token = createRestaurantUserWithRestaurant("Chez Jane");
        Long itemTypeId = firstItemTypeId(token);
        Long menuItemId = createMenuItem(token, "Soup", itemTypeId);

        mockMvc.perform(delete("/menu-items/" + menuItemId).header("Authorization", bearer(token)))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteMenuItem_returns404_whenBelongsToAnotherRestaurant() throws Exception {
        String tokenA = createRestaurantUserWithRestaurant("Restaurant A");
        String tokenB = createRestaurantUserWithRestaurant("Restaurant B");
        Long itemTypeId = firstItemTypeId(tokenA);
        Long menuItemId = createMenuItem(tokenA, "A's Soup", itemTypeId);

        mockMvc.perform(delete("/menu-items/" + menuItemId).header("Authorization", bearer(tokenB)))
                .andExpect(status().isNotFound());

        // never shows up in B's own listing either
        mockMvc.perform(get("/menu-items").header("Authorization", bearer(tokenB)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

}
