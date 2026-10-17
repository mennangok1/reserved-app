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
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ReservationFlowTest extends AbstractIntegrationTest {

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

    private Long createHold(String customerToken, Long tableId) throws Exception {
        MvcResult result = mockMvc.perform(post("/table-holds")
                        .header("Authorization", bearer(customerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TableHoldRequest(tableId))))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private String reservationBody(Long holdId, LocalDateTime start, LocalDateTime end) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "holdId", holdId,
                "startDate", start.toString(),
                "endDate", end.toString()
        ));
    }

    @Test
    void createReservation_returns201_andConsumesHold() throws Exception {
        String restaurantUserToken = createRestaurantUserWithRestaurant("Chez Jane");
        Long tableId = createTable(restaurantUserToken, "T1", 4L);
        String customerToken = registerCustomerAndLogin(uniqueEmail("customer"));
        Long holdId = createHold(customerToken, tableId);
        LocalDateTime start = LocalDateTime.now().plusDays(1);

        mockMvc.perform(post("/reservations")
                        .header("Authorization", bearer(customerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reservationBody(holdId, start, start.plusHours(2))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tableId").value(tableId))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        // the hold was consumed: releasing it again should 404
        mockMvc.perform(delete("/table-holds/" + holdId).header("Authorization", bearer(customerToken)))
                .andExpect(status().isNotFound());
    }

    @Test
    void createReservation_returns403_forRestaurantUser() throws Exception {
        String restaurantUserToken = createRestaurantUserWithRestaurant("Chez Jane");
        Long tableId = createTable(restaurantUserToken, "T1", 4L);
        String customerToken = registerCustomerAndLogin(uniqueEmail("customer"));
        Long holdId = createHold(customerToken, tableId);
        LocalDateTime start = LocalDateTime.now().plusDays(1);

        mockMvc.perform(post("/reservations")
                        .header("Authorization", bearer(restaurantUserToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reservationBody(holdId, start, start.plusHours(2))))
                .andExpect(status().isForbidden());
    }

    @Test
    void createReservation_returns404_whenHoldBelongsToAnotherCustomer() throws Exception {
        String restaurantUserToken = createRestaurantUserWithRestaurant("Chez Jane");
        Long tableId = createTable(restaurantUserToken, "T1", 4L);
        String ownerToken = registerCustomerAndLogin(uniqueEmail("customer"));
        String otherCustomerToken = registerCustomerAndLogin(uniqueEmail("customer"));
        Long holdId = createHold(ownerToken, tableId);
        LocalDateTime start = LocalDateTime.now().plusDays(1);

        mockMvc.perform(post("/reservations")
                        .header("Authorization", bearer(otherCustomerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reservationBody(holdId, start, start.plusHours(2))))
                .andExpect(status().isNotFound());
    }

    @Test
    void createReservation_returns409_whenHoldHasExpired() throws Exception {
        String restaurantUserToken = createRestaurantUserWithRestaurant("Chez Jane");
        Long tableId = createTable(restaurantUserToken, "T1", 4L);
        String customerToken = registerCustomerAndLogin(uniqueEmail("customer"));
        Long holdId = createHold(customerToken, tableId);

        TableHold hold = tableHoldRepository.findById(holdId).orElseThrow();
        hold.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        tableHoldRepository.save(hold);

        LocalDateTime start = LocalDateTime.now().plusDays(1);
        mockMvc.perform(post("/reservations")
                        .header("Authorization", bearer(customerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reservationBody(holdId, start, start.plusHours(2))))
                .andExpect(status().isConflict());
    }

    @Test
    void createReservation_returns409_whenOverlapsExistingActiveReservation() throws Exception {
        String restaurantUserToken = createRestaurantUserWithRestaurant("Chez Jane");
        Long tableId = createTable(restaurantUserToken, "T1", 4L);
        String customerAToken = registerCustomerAndLogin(uniqueEmail("customer"));
        Long holdAId = createHold(customerAToken, tableId);
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        LocalDateTime end = start.plusHours(2);

        mockMvc.perform(post("/reservations")
                        .header("Authorization", bearer(customerAToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reservationBody(holdAId, start, end)))
                .andExpect(status().isCreated());

        // customer A's hold was consumed, so the table is free to hold again, even though the
        // reservation itself still conflicts with the requested range below.
        String customerBToken = registerCustomerAndLogin(uniqueEmail("customer"));
        Long holdBId = createHold(customerBToken, tableId);
        LocalDateTime overlappingStart = start.plusHours(1);
        LocalDateTime overlappingEnd = overlappingStart.plusHours(2);

        mockMvc.perform(post("/reservations")
                        .header("Authorization", bearer(customerBToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reservationBody(holdBId, overlappingStart, overlappingEnd)))
                .andExpect(status().isConflict());
    }

    @Test
    void createReservation_returns201_whenDatesDoNotOverlapExistingReservation() throws Exception {
        String restaurantUserToken = createRestaurantUserWithRestaurant("Chez Jane");
        Long tableId = createTable(restaurantUserToken, "T1", 4L);
        String customerAToken = registerCustomerAndLogin(uniqueEmail("customer"));
        Long holdAId = createHold(customerAToken, tableId);
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        LocalDateTime end = start.plusHours(2);

        mockMvc.perform(post("/reservations")
                        .header("Authorization", bearer(customerAToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reservationBody(holdAId, start, end)))
                .andExpect(status().isCreated());

        String customerBToken = registerCustomerAndLogin(uniqueEmail("customer"));
        Long holdBId = createHold(customerBToken, tableId);
        LocalDateTime nonOverlappingStart = LocalDateTime.now().plusDays(3);
        LocalDateTime nonOverlappingEnd = nonOverlappingStart.plusHours(2);

        mockMvc.perform(post("/reservations")
                        .header("Authorization", bearer(customerBToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reservationBody(holdBId, nonOverlappingStart, nonOverlappingEnd)))
                .andExpect(status().isCreated());
    }

    @Test
    void listOwn_returnsOnlyCallersOwnReservations() throws Exception {
        String restaurantUserToken = createRestaurantUserWithRestaurant("Chez Jane");
        Long tableId = createTable(restaurantUserToken, "T1", 4L);
        String customerToken = registerCustomerAndLogin(uniqueEmail("customer"));
        Long holdId = createHold(customerToken, tableId);
        LocalDateTime start = LocalDateTime.now().plusDays(1);

        mockMvc.perform(post("/reservations")
                        .header("Authorization", bearer(customerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reservationBody(holdId, start, start.plusHours(2))))
                .andExpect(status().isCreated());

        String otherCustomerToken = registerCustomerAndLogin(uniqueEmail("customer"));

        mockMvc.perform(get("/reservations").header("Authorization", bearer(customerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        mockMvc.perform(get("/reservations").header("Authorization", bearer(otherCustomerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void cancelReservation_returns404_whenBelongsToAnotherCustomer() throws Exception {
        String restaurantUserToken = createRestaurantUserWithRestaurant("Chez Jane");
        Long tableId = createTable(restaurantUserToken, "T1", 4L);
        String ownerToken = registerCustomerAndLogin(uniqueEmail("customer"));
        String otherCustomerToken = registerCustomerAndLogin(uniqueEmail("customer"));
        Long holdId = createHold(ownerToken, tableId);
        LocalDateTime start = LocalDateTime.now().plusDays(1);

        MvcResult result = mockMvc.perform(post("/reservations")
                        .header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reservationBody(holdId, start, start.plusHours(2))))
                .andExpect(status().isCreated())
                .andReturn();
        Long reservationId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(delete("/reservations/" + reservationId).header("Authorization", bearer(otherCustomerToken)))
                .andExpect(status().isNotFound());
    }

    @Test
    void cancelReservation_returns204_whenCallerOwnsIt_andTableCanBeDeletedAfterwards() throws Exception {
        String restaurantUserToken = createRestaurantUserWithRestaurant("Chez Jane");
        Long tableId = createTable(restaurantUserToken, "T1", 4L);
        String customerToken = registerCustomerAndLogin(uniqueEmail("customer"));
        Long holdId = createHold(customerToken, tableId);
        LocalDateTime start = LocalDateTime.now().plusDays(1);

        MvcResult result = mockMvc.perform(post("/reservations")
                        .header("Authorization", bearer(customerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reservationBody(holdId, start, start.plusHours(2))))
                .andExpect(status().isCreated())
                .andReturn();
        Long reservationId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();

        // active reservation blocks table deletion (FK-guard)
        mockMvc.perform(delete("/restaurant-tables/" + tableId).header("Authorization", bearer(restaurantUserToken)))
                .andExpect(status().isConflict());

        mockMvc.perform(delete("/reservations/" + reservationId).header("Authorization", bearer(customerToken)))
                .andExpect(status().isNoContent());

        // once cancelled, the table can be deleted
        mockMvc.perform(delete("/restaurant-tables/" + tableId).header("Authorization", bearer(restaurantUserToken)))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteTable_succeeds_whenOnlyPastDatedReservationAndStaleExpiredHoldRemain() throws Exception {
        String restaurantUserToken = createRestaurantUserWithRestaurant("Chez Jane");
        Long tableId = createTable(restaurantUserToken, "T1", 4L);

        // an expired hold that was never released (ADR-006: expired holds are never physically
        // cleaned up) must not permanently block deletion either.
        String customerAToken = registerCustomerAndLogin(uniqueEmail("customer"));
        Long staleHoldId = createHold(customerAToken, tableId);
        TableHold staleHold = tableHoldRepository.findById(staleHoldId).orElseThrow();
        staleHold.setExpiresAt(LocalDateTime.now().minusDays(1));
        tableHoldRepository.save(staleHold);

        String customerBToken = registerCustomerAndLogin(uniqueEmail("customer"));
        Long holdId = createHold(customerBToken, tableId);
        LocalDateTime pastStart = LocalDateTime.now().minusDays(2);
        LocalDateTime pastEnd = pastStart.plusHours(2);

        MvcResult result = mockMvc.perform(post("/reservations")
                        .header("Authorization", bearer(customerBToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reservationBody(holdId, pastStart, pastEnd)))
                .andExpect(status().isCreated())
                .andReturn();
        Long reservationId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(delete("/restaurant-tables/" + tableId).header("Authorization", bearer(restaurantUserToken)))
                .andExpect(status().isNoContent());

        // the historical reservation is still listable (tableId now null), not a 500
        MvcResult listResult = mockMvc.perform(get("/reservations").header("Authorization", bearer(customerBToken)))
                .andExpect(status().isOk())
                .andReturn();
        var reservations = objectMapper.readTree(listResult.getResponse().getContentAsString());
        assertThat(reservations.get(0).get("id").asLong()).isEqualTo(reservationId);
        assertThat(reservations.get(0).get("tableId").isNull()).isTrue();
    }

    @Test
    void listTables_showsAvailabilityToCustomer() throws Exception {
        String restaurantUserToken = createRestaurantUserWithRestaurant("Chez Jane");
        MvcResult restaurantResult = mockMvc.perform(get("/restaurants/mine")
                        .header("Authorization", bearer(restaurantUserToken)))
                .andExpect(status().isOk())
                .andReturn();
        Long restaurantId = objectMapper.readTree(restaurantResult.getResponse().getContentAsString()).get("id").asLong();

        Long freeTableId = createTable(restaurantUserToken, "T1", 4L);
        Long heldTableId = createTable(restaurantUserToken, "T2", 2L);

        String customerToken = registerCustomerAndLogin(uniqueEmail("customer"));
        createHold(customerToken, heldTableId);

        mockMvc.perform(get("/restaurants/" + restaurantId + "/tables").header("Authorization", bearer(customerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        mockMvc.perform(get("/restaurants/" + restaurantId + "/tables").header("Authorization", bearer(customerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + freeTableId + ")].available").value(true))
                .andExpect(jsonPath("$[?(@.id == " + heldTableId + ")].available").value(false));
    }

    @Test
    void listTables_returns400_whenOnlyStartDateGiven() throws Exception {
        String restaurantUserToken = createRestaurantUserWithRestaurant("Chez Jane");
        MvcResult restaurantResult = mockMvc.perform(get("/restaurants/mine")
                        .header("Authorization", bearer(restaurantUserToken)))
                .andExpect(status().isOk())
                .andReturn();
        Long restaurantId = objectMapper.readTree(restaurantResult.getResponse().getContentAsString()).get("id").asLong();

        String customerToken = registerCustomerAndLogin(uniqueEmail("customer"));

        mockMvc.perform(get("/restaurants/" + restaurantId + "/tables")
                        .header("Authorization", bearer(customerToken))
                        .param("startDate", LocalDateTime.now().plusDays(1).toString()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listTables_showsDateRangeAvailability_ignoringPastOrUnrelatedReservation() throws Exception {
        String restaurantUserToken = createRestaurantUserWithRestaurant("Chez Jane");
        MvcResult restaurantResult = mockMvc.perform(get("/restaurants/mine")
                        .header("Authorization", bearer(restaurantUserToken)))
                .andExpect(status().isOk())
                .andReturn();
        Long restaurantId = objectMapper.readTree(restaurantResult.getResponse().getContentAsString()).get("id").asLong();

        Long tableId = createTable(restaurantUserToken, "T1", 4L);
        String customerToken = registerCustomerAndLogin(uniqueEmail("customer"));
        Long holdId = createHold(customerToken, tableId);
        LocalDateTime reservedStart = LocalDateTime.now().plusDays(1);
        LocalDateTime reservedEnd = reservedStart.plusHours(2);

        mockMvc.perform(post("/reservations")
                        .header("Authorization", bearer(customerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reservationBody(holdId, reservedStart, reservedEnd)))
                .andExpect(status().isCreated());

        // non-overlapping window: available, even though the table has an ACTIVE reservation elsewhere
        LocalDateTime freeStart = LocalDateTime.now().plusDays(3);
        mockMvc.perform(get("/restaurants/" + restaurantId + "/tables")
                        .header("Authorization", bearer(customerToken))
                        .param("startDate", freeStart.toString())
                        .param("endDate", freeStart.plusHours(2).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + tableId + ")].available").value(true));

        // overlapping window: unavailable
        LocalDateTime overlappingStart = reservedStart.plusHours(1);
        mockMvc.perform(get("/restaurants/" + restaurantId + "/tables")
                        .header("Authorization", bearer(customerToken))
                        .param("startDate", overlappingStart.toString())
                        .param("endDate", overlappingStart.plusHours(2).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + tableId + ")].available").value(false));
    }

}
