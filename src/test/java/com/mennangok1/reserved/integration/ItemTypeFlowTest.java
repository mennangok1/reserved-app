package com.mennangok1.reserved.integration;

import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ItemTypeFlowTest extends AbstractIntegrationTest {

    @Test
    void listItemTypes_returnsSeededTypes_whenAuthenticated() throws Exception {
        String token = registerCustomerAndLogin(uniqueEmail("customer"));

        mockMvc.perform(get("/item-types").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$[*].name")
                        .value(org.hamcrest.Matchers.hasItems("APPETIZER", "MAIN_COURSE", "DESSERT", "BEVERAGE")));
    }

    @Test
    void listItemTypes_returns401_whenNoToken() throws Exception {
        mockMvc.perform(get("/item-types"))
                .andExpect(status().isUnauthorized());
    }

}
