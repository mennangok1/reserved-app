package com.mennangok1.reserved.integration;

import com.mennangok1.reserved.restaurant.CreateRestaurantRequest;
import com.mennangok1.reserved.restaurantTable.RestaurantTableRequest;
import com.mennangok1.reserved.tableHold.TableHold;
import com.mennangok1.reserved.tableHold.TableHoldRepository;
import com.mennangok1.reserved.tableHold.TableHoldRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TableHoldFlowTest extends AbstractIntegrationTest {

    @Autowired
    private TableHoldRepository tableHoldRepository;

    private String createRestaurantUserWithRestaurant(String name) throws Exception {
        String token = registerRestaurantUserAndLogin(uniqueEmail("restaurant-user"));
        mockMvc.perform(post("/restaurants")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateRestaurantRequest(name, null))))
                .andExpect(status().isCreated());
        return token;
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

    @Test
    void createHold_returns201_forCustomer() throws Exception {
        String restaurantUserToken = createRestaurantUserWithRestaurant("Chez Jane");
        Long tableId = createTable(restaurantUserToken, "T1", 4L);
        String customerToken = registerCustomerAndLogin(uniqueEmail("customer"));

        mockMvc.perform(post("/table-holds")
                        .header("Authorization", bearer(customerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TableHoldRequest(tableId))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tableId").value(tableId));
    }

    @Test
    void createHold_returns403_forRestaurantUser() throws Exception {
        String restaurantUserToken = createRestaurantUserWithRestaurant("Chez Jane");
        Long tableId = createTable(restaurantUserToken, "T1", 4L);

        mockMvc.perform(post("/table-holds")
                        .header("Authorization", bearer(restaurantUserToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TableHoldRequest(tableId))))
                .andExpect(status().isForbidden());
    }

    @Test
    void createHold_returns404_whenTableDoesNotExist() throws Exception {
        String customerToken = registerCustomerAndLogin(uniqueEmail("customer"));

        mockMvc.perform(post("/table-holds")
                        .header("Authorization", bearer(customerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TableHoldRequest(999999L))))
                .andExpect(status().isNotFound());
    }

    @Test
    void createHold_returns409_whenTableAlreadyHeldByAnotherCustomer() throws Exception {
        String restaurantUserToken = createRestaurantUserWithRestaurant("Chez Jane");
        Long tableId = createTable(restaurantUserToken, "T1", 4L);
        String firstCustomerToken = registerCustomerAndLogin(uniqueEmail("customer"));
        String secondCustomerToken = registerCustomerAndLogin(uniqueEmail("customer"));

        mockMvc.perform(post("/table-holds")
                        .header("Authorization", bearer(firstCustomerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TableHoldRequest(tableId))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/table-holds")
                        .header("Authorization", bearer(secondCustomerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TableHoldRequest(tableId))))
                .andExpect(status().isConflict());
    }

    @Test
    void createHold_succeeds_whenOnlyPriorHoldOnTableHasExpired() throws Exception {
        String restaurantUserToken = createRestaurantUserWithRestaurant("Chez Jane");
        Long tableId = createTable(restaurantUserToken, "T1", 4L);
        String firstCustomerToken = registerCustomerAndLogin(uniqueEmail("customer"));
        String secondCustomerToken = registerCustomerAndLogin(uniqueEmail("customer"));

        MvcResult result = mockMvc.perform(post("/table-holds")
                        .header("Authorization", bearer(firstCustomerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TableHoldRequest(tableId))))
                .andExpect(status().isCreated())
                .andReturn();
        Long holdId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();

        TableHold hold = tableHoldRepository.findById(holdId).orElseThrow();
        hold.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        tableHoldRepository.save(hold);

        mockMvc.perform(post("/table-holds")
                        .header("Authorization", bearer(secondCustomerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TableHoldRequest(tableId))))
                .andExpect(status().isCreated());
    }

    @Test
    void releaseHold_returns204_whenCallerOwnsIt() throws Exception {
        String restaurantUserToken = createRestaurantUserWithRestaurant("Chez Jane");
        Long tableId = createTable(restaurantUserToken, "T1", 4L);
        String customerToken = registerCustomerAndLogin(uniqueEmail("customer"));

        MvcResult result = mockMvc.perform(post("/table-holds")
                        .header("Authorization", bearer(customerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TableHoldRequest(tableId))))
                .andExpect(status().isCreated())
                .andReturn();
        Long holdId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(delete("/table-holds/" + holdId).header("Authorization", bearer(customerToken)))
                .andExpect(status().isNoContent());
    }

    @Test
    void releaseHold_returns404_whenHoldBelongsToAnotherCustomer() throws Exception {
        String restaurantUserToken = createRestaurantUserWithRestaurant("Chez Jane");
        Long tableId = createTable(restaurantUserToken, "T1", 4L);
        String ownerToken = registerCustomerAndLogin(uniqueEmail("customer"));
        String otherCustomerToken = registerCustomerAndLogin(uniqueEmail("customer"));

        MvcResult result = mockMvc.perform(post("/table-holds")
                        .header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TableHoldRequest(tableId))))
                .andExpect(status().isCreated())
                .andReturn();
        Long holdId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(delete("/table-holds/" + holdId).header("Authorization", bearer(otherCustomerToken)))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteTable_returns409_whenActiveHoldExists() throws Exception {
        String restaurantUserToken = createRestaurantUserWithRestaurant("Chez Jane");
        Long tableId = createTable(restaurantUserToken, "T1", 4L);
        String customerToken = registerCustomerAndLogin(uniqueEmail("customer"));

        mockMvc.perform(post("/table-holds")
                        .header("Authorization", bearer(customerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TableHoldRequest(tableId))))
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/restaurant-tables/" + tableId).header("Authorization", bearer(restaurantUserToken)))
                .andExpect(status().isConflict());
    }

}
