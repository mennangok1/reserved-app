package com.mennangok1.reserved.integration;

import com.mennangok1.reserved.TestcontainersConfiguration;
import com.mennangok1.reserved.user.LoginRequest;
import com.mennangok1.reserved.user.LoginResponse;
import com.mennangok1.reserved.user.RegisterRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Mirrors the manual requestCollection/*.http coverage from ADR-001/ADR-002 as JUnit tests.
// MockMvc dispatches in-thread, so @Transactional rolls back every test's writes against the
// shared Testcontainers Postgres instance without needing a fresh container per test.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
abstract class AbstractIntegrationTest {

    protected static final String DEFAULT_PASSWORD = "password123";

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    protected String uniqueEmail(String prefix) {
        return prefix + "-" + UUID.randomUUID() + "@test.com";
    }

    protected String registerCustomerAndLogin(String email) throws Exception {
        return registerAndLogin("/auth/register", email);
    }

    protected String registerRestaurantUserAndLogin(String email) throws Exception {
        return registerAndLogin("/auth/register/restaurant-user", email);
    }

    private String registerAndLogin(String registerPath, String email) throws Exception {
        RegisterRequest registerRequest = new RegisterRequest("Test User", email, DEFAULT_PASSWORD, null);
        mockMvc.perform(post(registerPath)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        return login(email, DEFAULT_PASSWORD);
    }

    protected String login(String email, String password) throws Exception {
        LoginRequest loginRequest = new LoginRequest(email, password);
        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        LoginResponse loginResponse = objectMapper.readValue(
                result.getResponse().getContentAsString(), LoginResponse.class);
        return loginResponse.token();
    }

    protected String bearer(String token) {
        return "Bearer " + token;
    }

    // Item types are a seeded lookup table (V7 migration) — fetched dynamically rather than
    // hardcoding IDs so these tests don't depend on seed/insertion order.
    protected Long firstItemTypeId(String token) throws Exception {
        MvcResult result = mockMvc.perform(get("/item-types").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode items = objectMapper.readTree(result.getResponse().getContentAsString());
        return items.get(0).get("id").asLong();
    }

}
