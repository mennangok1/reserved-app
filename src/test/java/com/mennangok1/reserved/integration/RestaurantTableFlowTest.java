package com.mennangok1.reserved.integration;

import com.mennangok1.reserved.restaurant.CreateRestaurantRequest;
import com.mennangok1.reserved.restaurantTable.RestaurantTableRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RestaurantTableFlowTest extends AbstractIntegrationTest {

    private String createRestaurantUserWithRestaurant(String name) throws Exception {
        String token = registerRestaurantUserAndLogin(uniqueEmail("restaurant-user"));
        mockMvc.perform(post("/restaurants")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateRestaurantRequest(name, null))))
                .andExpect(status().isCreated());
        return token;
    }

    private Long createTable(String token, String label, Long capacity) throws Exception {
        RestaurantTableRequest request = new RestaurantTableRequest(label, capacity);
        MvcResult result = mockMvc.perform(post("/restaurant-tables")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    @Test
    void createTable_returns201_forOwnRestaurant() throws Exception {
        String token = createRestaurantUserWithRestaurant("Chez Jane");

        RestaurantTableRequest request = new RestaurantTableRequest("T1", 4L);

        mockMvc.perform(post("/restaurant-tables")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.label").value("T1"))
                .andExpect(jsonPath("$.capacity").value(4));
    }

    @Test
    void createTable_returns403_forCustomer() throws Exception {
        String customerToken = registerCustomerAndLogin(uniqueEmail("customer"));

        RestaurantTableRequest request = new RestaurantTableRequest("T1", 4L);

        mockMvc.perform(post("/restaurant-tables")
                        .header("Authorization", bearer(customerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void listOwn_returnsOnlyCallersOwnRestaurantTables() throws Exception {
        String tokenA = createRestaurantUserWithRestaurant("Restaurant A");
        String tokenB = createRestaurantUserWithRestaurant("Restaurant B");

        createTable(tokenA, "A1", 2L);
        createTable(tokenB, "B1", 2L);

        mockMvc.perform(get("/restaurant-tables").header("Authorization", bearer(tokenA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].label").value("A1"));
    }

    @Test
    void updateTable_returns200_whenCallerOwnsIt() throws Exception {
        String token = createRestaurantUserWithRestaurant("Chez Jane");
        Long tableId = createTable(token, "T1", 4L);

        RestaurantTableRequest update = new RestaurantTableRequest("T1-renamed", 6L);

        mockMvc.perform(put("/restaurant-tables/" + tableId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.label").value("T1-renamed"))
                .andExpect(jsonPath("$.capacity").value(6));
    }

    @Test
    void updateTable_returns404_whenBelongsToAnotherRestaurant() throws Exception {
        String tokenA = createRestaurantUserWithRestaurant("Restaurant A");
        String tokenB = createRestaurantUserWithRestaurant("Restaurant B");
        Long tableId = createTable(tokenA, "A1", 2L);

        RestaurantTableRequest update = new RestaurantTableRequest("Hijacked", 1L);

        mockMvc.perform(put("/restaurant-tables/" + tableId)
                        .header("Authorization", bearer(tokenB))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteTable_returns204_whenCallerOwnsIt() throws Exception {
        String token = createRestaurantUserWithRestaurant("Chez Jane");
        Long tableId = createTable(token, "T1", 4L);

        mockMvc.perform(delete("/restaurant-tables/" + tableId).header("Authorization", bearer(token)))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteTable_returns404_whenBelongsToAnotherRestaurant() throws Exception {
        String tokenA = createRestaurantUserWithRestaurant("Restaurant A");
        String tokenB = createRestaurantUserWithRestaurant("Restaurant B");
        Long tableId = createTable(tokenA, "A1", 2L);

        mockMvc.perform(delete("/restaurant-tables/" + tableId).header("Authorization", bearer(tokenB)))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/restaurant-tables").header("Authorization", bearer(tokenB)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

}
