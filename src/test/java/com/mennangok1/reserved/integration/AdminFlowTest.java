package com.mennangok1.reserved.integration;

import com.mennangok1.reserved.restaurant.CreateRestaurantRequest;
import com.mennangok1.reserved.restaurantTable.RestaurantTableRequest;
import com.mennangok1.reserved.tableHold.TableHoldRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// ADMIN slice (ADR-007 next step): browsing across restaurants, user listing, and runtime
// permission management (grant/revoke), closing the @CacheEvict gap from ADR-004/007.
class AdminFlowTest extends AbstractIntegrationTest {

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

    private Long createTable(String restaurantUserToken, String label, Long capacity) throws Exception {
        RestaurantTableRequest request = new RestaurantTableRequest(label, capacity);
        MvcResult result = mockMvc.perform(post("/restaurant-tables")
                        .header("Authorization", bearer(restaurantUserToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private Long createHold(String customerToken, Long tableId) throws Exception {
        MvcResult result = mockMvc.perform(post("/table-holds")
                        .header("Authorization", bearer(customerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TableHoldRequest(tableId))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private void createReservation(String customerToken, Long holdId) throws Exception {
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        String body = objectMapper.writeValueAsString(Map.of(
                "holdId", holdId,
                "startDate", start.toString(),
                "endDate", start.plusHours(2).toString()
        ));
        mockMvc.perform(post("/reservations")
                        .header("Authorization", bearer(customerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());
    }

    // --- Browsing across restaurants (already-granted RESTAURANT_READ/MENU_ITEM_READ, new RESERVATION_READ) ---

    @Test
    void listRestaurants_returns200_forAdmin() throws Exception {
        createRestaurantUserWithRestaurant("Chez Jane");
        String adminToken = loginAsSeededAdmin();

        mockMvc.perform(get("/restaurants").header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk());
    }

    @Test
    void getRestaurant_returns200_forAdmin() throws Exception {
        String ownerToken = createRestaurantUserWithRestaurant("Chez Jane");
        Long restaurantId = ownRestaurantId(ownerToken);
        String adminToken = loginAsSeededAdmin();

        mockMvc.perform(get("/restaurants/" + restaurantId).header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Chez Jane"));
    }

    @Test
    void listMenuItems_returns200_forAdmin_forAnotherRestaurantsMenu() throws Exception {
        String ownerToken = createRestaurantUserWithRestaurant("Chez Jane");
        Long restaurantId = ownRestaurantId(ownerToken);
        String adminToken = loginAsSeededAdmin();

        mockMvc.perform(get("/restaurants/" + restaurantId + "/menu-items").header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk());
    }

    @Test
    void listTables_returns200_forAdmin() throws Exception {
        String ownerToken = createRestaurantUserWithRestaurant("Chez Jane");
        Long restaurantId = ownRestaurantId(ownerToken);
        createTable(ownerToken, "T1", 4L);
        String adminToken = loginAsSeededAdmin();

        mockMvc.perform(get("/restaurants/" + restaurantId + "/tables").header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void listReservations_returns200_forAdmin_acrossAnyRestaurant() throws Exception {
        String ownerToken = createRestaurantUserWithRestaurant("Chez Jane");
        Long restaurantId = ownRestaurantId(ownerToken);
        Long tableId = createTable(ownerToken, "T1", 4L);
        String customerToken = registerCustomerAndLogin(uniqueEmail("customer"));
        Long holdId = createHold(customerToken, tableId);
        createReservation(customerToken, holdId);

        String adminToken = loginAsSeededAdmin();

        mockMvc.perform(get("/restaurants/" + restaurantId + "/reservations").header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].tableId").value(tableId));
    }

    @Test
    void listReservations_returns404_forAdmin_whenRestaurantMissing() throws Exception {
        String adminToken = loginAsSeededAdmin();

        mockMvc.perform(get("/restaurants/999999/reservations").header("Authorization", bearer(adminToken)))
                .andExpect(status().isNotFound());
    }

    @Test
    void listReservations_returns403_forCustomer() throws Exception {
        String ownerToken = createRestaurantUserWithRestaurant("Chez Jane");
        Long restaurantId = ownRestaurantId(ownerToken);
        String customerToken = registerCustomerAndLogin(uniqueEmail("customer"));

        mockMvc.perform(get("/restaurants/" + restaurantId + "/reservations").header("Authorization", bearer(customerToken)))
                .andExpect(status().isForbidden());
    }

    // --- GET /users ---

    @Test
    void listUsers_returns200_forAdmin_andNeverExposesPassword() throws Exception {
        createRestaurantUserWithRestaurant("Chez Jane");
        registerCustomerAndLogin(uniqueEmail("customer"));
        String adminToken = loginAsSeededAdmin();

        mockMvc.perform(get("/users").header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.password)]").doesNotExist())
                .andExpect(jsonPath("$[?(@.role == 'ADMIN')]").exists())
                .andExpect(jsonPath("$[?(@.role == 'RESTAURANT_USER')]").exists())
                .andExpect(jsonPath("$[?(@.role == 'CUSTOMER')]").exists());
    }

    @Test
    void listUsers_returns403_forCustomer() throws Exception {
        String customerToken = registerCustomerAndLogin(uniqueEmail("customer"));

        mockMvc.perform(get("/users").header("Authorization", bearer(customerToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    void listUsers_returns403_forRestaurantUser() throws Exception {
        String restaurantUserToken = createRestaurantUserWithRestaurant("Chez Jane");

        mockMvc.perform(get("/users").header("Authorization", bearer(restaurantUserToken)))
                .andExpect(status().isForbidden());
    }

    // --- Permission Management (grant/revoke), and the @CacheEvict regression test ---

    @Test
    void grantThenUseThenRevokePermission_takesEffectWithoutRestart() throws Exception {
        String customerToken = registerCustomerAndLogin(uniqueEmail("customer"));
        String adminToken = loginAsSeededAdmin();

        // CUSTOMER has no ROLE_ACTION_READ (ADMIN-only, V12) -> forbidden up front. /actions has
        // only a permission check (no ownership-resolution step), so this isolates the cache fix.
        mockMvc.perform(get("/actions").header("Authorization", bearer(customerToken)))
                .andExpect(status().isForbidden());

        String grantBody = objectMapper.writeValueAsString(Map.of(
                "roleName", "CUSTOMER", "actionName", "ROLE_ACTION_READ"));
        mockMvc.perform(post("/role-actions")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(grantBody))
                .andExpect(status().isCreated());

        // Same already-logged-in customer token, no restart: proves the cache was actually
        // populated and then evicted on grant, not just that the HTTP codes look right.
        mockMvc.perform(get("/actions").header("Authorization", bearer(customerToken)))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/role-actions/CUSTOMER/ROLE_ACTION_READ")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/actions").header("Authorization", bearer(customerToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    void grantPermission_returns409_whenAlreadyGranted() throws Exception {
        String adminToken = loginAsSeededAdmin();

        String grantBody = objectMapper.writeValueAsString(Map.of(
                "roleName", "ADMIN", "actionName", "USER_READ"));
        mockMvc.perform(post("/role-actions")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(grantBody))
                .andExpect(status().isConflict());
    }

    @Test
    void revokePermission_returns404_whenNotGranted() throws Exception {
        String adminToken = loginAsSeededAdmin();

        mockMvc.perform(delete("/role-actions/CUSTOMER/MENU_ITEM_CREATE")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isNotFound());
    }

    @Test
    void grantPermission_returns403_forNonAdmin() throws Exception {
        String customerToken = registerCustomerAndLogin(uniqueEmail("customer"));

        String grantBody = objectMapper.writeValueAsString(Map.of(
                "roleName", "CUSTOMER", "actionName", "RESTAURANT_TABLE_READ"));
        mockMvc.perform(post("/role-actions")
                        .header("Authorization", bearer(customerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(grantBody))
                .andExpect(status().isForbidden());
    }

    @Test
    void listActions_returns200_forAdmin() throws Exception {
        String adminToken = loginAsSeededAdmin();

        mockMvc.perform(get("/actions").header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name == 'USER_READ')]").exists());
    }

}
