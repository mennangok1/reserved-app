package com.mennangok1.reserved.integration;

import com.mennangok1.reserved.restaurant.CreateRestaurantRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RestaurantOnboardingFlowTest extends AbstractIntegrationTest {

    @Test
    void createRestaurant_returns201_forRestaurantUserWithoutOneYet() throws Exception {
        String token = registerRestaurantUserAndLogin(uniqueEmail("restaurant-user"));
        CreateRestaurantRequest request = new CreateRestaurantRequest("Chez Jane", "French bistro");

        mockMvc.perform(post("/restaurants")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Chez Jane"));
    }

    @Test
    void createRestaurant_returns409_whenUserAlreadyHasOne() throws Exception {
        String token = registerRestaurantUserAndLogin(uniqueEmail("restaurant-user"));
        CreateRestaurantRequest request = new CreateRestaurantRequest("Chez Jane", null);

        mockMvc.perform(post("/restaurants")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/restaurants")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateRestaurantRequest("Second Place", null))))
                .andExpect(status().isConflict());
    }

    @Test
    void createRestaurant_returns403_forCustomer() throws Exception {
        String token = registerCustomerAndLogin(uniqueEmail("customer"));
        CreateRestaurantRequest request = new CreateRestaurantRequest("Chez Jane", null);

        mockMvc.perform(post("/restaurants")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void getOwnRestaurant_returns200_whenAlreadyCreated() throws Exception {
        String token = registerRestaurantUserAndLogin(uniqueEmail("restaurant-user"));
        mockMvc.perform(post("/restaurants")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateRestaurantRequest("Chez Jane", null))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/restaurants/mine").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Chez Jane"));
    }

    @Test
    void getOwnRestaurant_returns404_whenNoneCreatedYet() throws Exception {
        String token = registerRestaurantUserAndLogin(uniqueEmail("restaurant-user"));

        mockMvc.perform(get("/restaurants/mine").header("Authorization", bearer(token)))
                .andExpect(status().isNotFound());
    }

}
