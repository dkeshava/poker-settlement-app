package com.dk.pokersettlement.integration;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class SettlementIntegrationTest extends IntegrationTestBase {

    private record SettlementFixture(
            String gameId,
            String participantA,
            String participantB,
            String usernameA,
            String passwordA
    ) {}

    private SettlementFixture prepareEndedGame() throws Exception {

        String gameId = createGame(managerToken);

        String usernameA = uniqueUsername();
        String usernameB = uniqueUsername();
        String password = "StrongPassword123!";

        Long userA = registerUser(usernameA, password);
        Long userB = registerUser(usernameB, password);

        String participantA = addParticipant(gameId, managerToken, userA);
        String participantB = addParticipant(gameId, managerToken, userB);

        addBuyIn(gameId, participantA, managerToken, 1000);
        addBuyIn(gameId, participantB, managerToken, 1000);

        startGame(gameId, managerToken);
        setRoundActive(gameId, managerToken,false);
        endGame(gameId, managerToken);

        return new SettlementFixture(
                gameId, participantA, participantB, usernameA, password
        );
    }

    private void submitBalancedFinalValues(
            SettlementFixture fixture
    ) throws Exception {

        String body = """
                {
                    "finalChipValues": [
                        {
                            "participantId": %s,
                            "chipValue": 500
                        },
                        {
                            "participantId": %s,
                            "chipValue": 1500
                        }
                    ]
                }
                """.formatted(
                fixture.participantA(),
                fixture.participantB()
        );

        mockMvc.perform(put("/games/" + fixture.gameId()
                        + "/final-chip-values")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
    }

    @Test
    void managerCanGetSettlementPreview() throws Exception {

        SettlementFixture fixture = prepareEndedGame();

        submitBalancedFinalValues(fixture);

        mockMvc.perform(get("/games/" + fixture.gameId()
                        + "/settlement-preview")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gameId")
                        .value(fixture.gameId()))
                .andExpect(jsonPath("$.chipConversionRate")
                        .value(0.10))
                .andExpect(jsonPath("$.netBalances.length()")
                        .value(2))
                .andExpect(jsonPath("$.transactionCount")
                        .value(1))
                .andExpect(jsonPath("$.transfers.length()")
                        .value(1))
                .andExpect(jsonPath("$.transfers[0].amount")
                        .value(50.00));
    }

    @Test
    void settlementPreviewContainsCorrectParticipants()
            throws Exception {

        SettlementFixture fixture = prepareEndedGame();

        submitBalancedFinalValues(fixture);

        MvcResult result = mockMvc.perform(
                        get("/games/" + fixture.gameId()
                                + "/settlement-preview")
                                .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andReturn();

        tools.jackson.databind.JsonNode response = objectMapper.readTree(
                result.getResponse().getContentAsString()
        );

        tools.jackson.databind.JsonNode balances = response.get("netBalances");

        assertEquals(2, balances.size());

        boolean foundLoser = false;
        boolean foundWinner = false;

        for (JsonNode balance : balances) {

            String participantId =
                    balance.get("participantId").asText();

            long netChips =
                    balance.get("netChipValue").asLong();

            if (participantId.equals(fixture.participantA())) {
                assertEquals(-500, netChips);
                assertEquals(-50.0,
                        balance.get("netAmount").asDouble(), 0.001);
                foundLoser = true;
            }

            if (participantId.equals(fixture.participantB())) {
                assertEquals(500, netChips);
                assertEquals(50.0,
                        balance.get("netAmount").asDouble(), 0.001);
                foundWinner = true;
            }
        }

        assertTrue(foundLoser);
        assertTrue(foundWinner);
    }

    @Test
    void accountingMismatchPreventsSettlementPreview()
            throws Exception {

        SettlementFixture fixture = prepareEndedGame();

        String body = """
                {
                    "finalChipValues": [
                        {
                            "participantId": %s,
                            "chipValue": 400
                        },
                        {
                            "participantId": %s,
                            "chipValue": 1400
                        }
                    ]
                }
                """.formatted(
                fixture.participantA(),
                fixture.participantB()
        );

        mockMvc.perform(put("/games/" + fixture.gameId()
                        + "/final-chip-values")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        // Buy-ins = 2000, final chips = 1800.
        mockMvc.perform(get("/games/" + fixture.gameId()
                        + "/settlement-preview")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void previewCannotBeGeneratedBeforeFinalChipValuesAreSubmitted()
            throws Exception {

        SettlementFixture fixture = prepareEndedGame();

        mockMvc.perform(get("/games/" + fixture.gameId()
                        + "/settlement-preview")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void managerCanFinalizeSettlement() throws Exception {

        SettlementFixture fixture = prepareEndedGame();

        submitBalancedFinalValues(fixture);

        mockMvc.perform(post("/games/" + fixture.gameId()
                        + "/settlement/finalize")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gameId")
                        .value(fixture.gameId()))
                .andExpect(jsonPath("$.transactionCount")
                        .value(1))
                .andExpect(jsonPath("$.transfers.length()")
                        .value(1))
                .andExpect(jsonPath("$.transfers[0].amount")
                        .value(50.00));
    }

    @Test
    void finalizedSettlementCanBeRetrieved() throws Exception {

        SettlementFixture fixture = prepareEndedGame();

        submitBalancedFinalValues(fixture);

        mockMvc.perform(post("/games/" + fixture.gameId()
                        + "/settlement/finalize")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/games/" + fixture.gameId()
                        + "/settlement")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gameId")
                        .value(fixture.gameId()))
                .andExpect(jsonPath("$.transactionCount")
                        .value(1))
                .andExpect(jsonPath("$.transfers.length()")
                        .value(1))
                .andExpect(jsonPath("$.transfers[0].amount")
                        .value(50.00));
    }

    @Test
    void outsiderCannotGetSettlementPreview() throws Exception {

        SettlementFixture fixture = prepareEndedGame();

        submitBalancedFinalValues(fixture);

        String outsiderToken = registerAndLoginParticipant();

        mockMvc.perform(get("/games/" + fixture.gameId()
                        + "/settlement-preview")
                        .header("Authorization", "Bearer " + outsiderToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void outsiderCannotFinalizeSettlement() throws Exception {

        SettlementFixture fixture = prepareEndedGame();

        submitBalancedFinalValues(fixture);

        String outsiderToken = registerAndLoginParticipant();

        mockMvc.perform(post("/games/" + fixture.gameId()
                        + "/settlement/finalize")
                        .header("Authorization", "Bearer " + outsiderToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void participantCanViewSettlementAfterFinalization() throws Exception {

        SettlementFixture fixture = prepareEndedGame();

        submitBalancedFinalValues(fixture);

        mockMvc.perform(post("/games/" + fixture.gameId()
                        + "/settlement/finalize")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk());

        String participantToken = loginUser(
                fixture.usernameA(), fixture.passwordA()
        );

        mockMvc.perform(get("/games/" + fixture.gameId() + "/settlement")
                        .header("Authorization", "Bearer " + participantToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionCount").value(1));
    }
}

