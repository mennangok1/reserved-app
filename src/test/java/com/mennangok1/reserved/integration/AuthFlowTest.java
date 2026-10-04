package com.mennangok1.reserved.integration;

import com.mennangok1.reserved.user.LoginRequest;
import com.mennangok1.reserved.user.RegisterRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthFlowTest extends AbstractIntegrationTest {

    @Test
    void registerCustomer_returns201WithCustomerRole() throws Exception {
        String email = uniqueEmail("customer");
        RegisterRequest request = new RegisterRequest("Jane Doe", email, DEFAULT_PASSWORD, "5551234567");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.role").value("CUSTOMER"));
    }

    @Test
    void registerCustomer_returns409_whenEmailAlreadyRegistered() throws Exception {
        String email = uniqueEmail("customer");
        RegisterRequest request = new RegisterRequest("Jane Doe", email, DEFAULT_PASSWORD, null);

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void registerRestaurantUser_returns201WithRestaurantUserRole() throws Exception {
        String email = uniqueEmail("restaurant-user");
        RegisterRequest request = new RegisterRequest("Remy", email, DEFAULT_PASSWORD, null);

        mockMvc.perform(post("/auth/register/restaurant-user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("RESTAURANT_USER"));
    }

    @Test
    void login_returns200WithToken_whenCredentialsValid() throws Exception {
        String email = uniqueEmail("customer");
        registerCustomerAndLogin(email); // proves register->login already works; re-login below for the assertion

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(email, DEFAULT_PASSWORD))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void login_returns401_whenPasswordWrong() throws Exception {
        String email = uniqueEmail("customer");
        registerCustomerAndLogin(email);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(email, "wrong-password"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_returns401_whenUserDoesNotExist() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(uniqueEmail("ghost"), DEFAULT_PASSWORD))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void me_returns200WithUserData_whenAuthenticated() throws Exception {
        String email = uniqueEmail("customer");
        String token = registerCustomerAndLogin(email);

        mockMvc.perform(get("/auth/me").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email));
    }

    @Test
    void me_returns401_whenNoToken() throws Exception {
        mockMvc.perform(get("/auth/me"))
                .andExpect(status().isUnauthorized());
    }

}
