package com.dk.pokersettlement.integration;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class GameIntegrationTest extends IntegrationTestBase {

    @Test
    void managerCanCreateGame() throws Exception {

        String gameId = createGame(managerToken);

        mockMvc.perform(get("/games/" + gameId)
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(gameId))
                .andExpect(jsonPath("$.gameStatus").value("OPEN"))
                .andExpect(jsonPath("$.roundActive").value(false))
                .andExpect(jsonPath("$.createdById").value(managerId));
    }

    @Test
    void outsiderCannotAccessGame() throws Exception {

        String gameId = createGame(managerToken);
        String outsiderToken = registerAndLoginParticipant();

        mockMvc.perform(get("/games/" + gameId)
                        .header("Authorization", "Bearer " + outsiderToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void participantCanAccessGame() throws Exception {

        String gameId = createGame(managerToken);

        String participantUsername = uniqueUsername();
        Long participantUserId = registerUser(
                participantUsername, "StrongPassword123!"
        );

        String participantToken = loginUser(
                participantUsername, "StrongPassword123!"
        );

        addParticipant(gameId, managerToken, participantUserId);

        mockMvc.perform(get("/games/" + gameId)
                        .header("Authorization", "Bearer " + participantToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(gameId));
    }

    @Test
    void missingGameReturns404() throws Exception {

        mockMvc.perform(get("/games/999999")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void gameLifecycleWorksCorrectly() throws Exception {

        String gameId = createGame(managerToken);

        String participantUsername = uniqueUsername();
        Long participantUserId = registerUser(
                participantUsername, "StrongPassword123!"
        );

        String participantId = addParticipant(
                gameId, managerToken, participantUserId
        );

        addBuyIn(gameId, participantId, managerToken, 1000);

        String participantUsername2 = uniqueUsername();

        Long participantUserId2 = registerUser(
                participantUsername2, "StrongPassword123!"
        );

        String participantId2 = addParticipant(
                gameId, managerToken, participantUserId2
        );

        addBuyIn(gameId, participantId2, managerToken, 1000);

        // OPEN -> RUNNING
        startGame(gameId, managerToken);

        mockMvc.perform(get("/games/" + gameId)
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gameStatus").value("RUNNING"))
                .andExpect(jsonPath("$.roundActive").value(true));

// Deactivate the round
        setRoundActive(gameId, managerToken, false);

        mockMvc.perform(get("/games/" + gameId)
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roundActive").value(false));

// RUNNING -> ENDED
        endGame(gameId, managerToken);

        mockMvc.perform(get("/games/" + gameId)
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gameStatus").value("ENDED"));

// Final chip value
        submitFinalChips(
                gameId,
                managerToken,
                new FinalChipEntry(Long.parseLong(participantId), 1000),
                new FinalChipEntry(Long.parseLong(participantId2), 1000)
        );
    }

    @Test
    void nonManagerCannotStartGame() throws Exception {

        String gameId = createGame(managerToken);
        String outsiderToken = registerAndLoginParticipant();

        mockMvc.perform(post("/games/" + gameId + "/start")
                        .header("Authorization", "Bearer " + outsiderToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void finalChipValuesCannotBeSubmittedBeforeGameEnds()
            throws Exception {

        String gameId = createGame(managerToken);

        String participantUsername = uniqueUsername();
        Long participantUserId = registerUser(
                participantUsername, "StrongPassword123!"
        );

        String participantId = addParticipant(
                gameId, managerToken, participantUserId
        );

        addBuyIn(gameId, participantId, managerToken, 1000);

        String body = """
                {
                    "finalChipValues": [
                        {
                            "participantId": %s,
                            "chipValue": 1000
                        }
                    ]
                }
                """.formatted(participantId);

        mockMvc.perform(put("/games/" + gameId + "/final-chip-values")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void negativeFinalChipValueIsRejected() throws Exception {

        String gameId = createGame(managerToken);

        // Participant 1
        String username = uniqueUsername();
        Long userId = registerUser(username, "StrongPassword123!");
        String participantId = addParticipant(gameId, managerToken, userId);

        addBuyIn(gameId, participantId, managerToken, 1000);

        // Participant 2
        String username2 = uniqueUsername();
        Long userId2 = registerUser(username2, "StrongPassword123!");
        String participantId2 = addParticipant(gameId, managerToken, userId2);

        addBuyIn(gameId, participantId2, managerToken, 1000);

        startGame(gameId, managerToken);
        // Stop the active round
        setRoundActive(gameId, managerToken, false);
        endGame(gameId, managerToken);

        String body = """
            {
                "finalChipValues": [
                    {
                        "participantId": %s,
                        "chipValue": -100
                    },
                    {
                        "participantId": %s,
                        "chipValue": 2100
                    }
                ]
            }
            """.formatted(participantId, participantId2);

        mockMvc.perform(put("/games/" + gameId + "/final-chip-values")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void managerCanEndGameWhenRoundIsInactive() throws Exception {

        String gameId = createGame(managerToken);

        String participant1Username = uniqueUsername();
        Long participant1UserId = registerUser(
                participant1Username, "StrongPassword123!"
        );

        String participant2Username = uniqueUsername();
        Long participant2UserId = registerUser(
                participant2Username, "StrongPassword123!"
        );

        String participant1Id = addParticipant(
                gameId, managerToken, participant1UserId
        );

        String participant2Id = addParticipant(
                gameId, managerToken, participant2UserId
        );

        addBuyIn(gameId, participant1Id, managerToken, 1000);
        addBuyIn(gameId, participant2Id, managerToken, 1000);

        startGame(gameId, managerToken);

        setRoundActive(gameId, managerToken, false);

        endGame(gameId, managerToken);

        mockMvc.perform(get("/games/" + gameId)
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gameStatus").value("ENDED"));
    }

    @Test
    void managerCannotEndGameWhenRoundIsActive() throws Exception {

        String gameId = createGame(managerToken);

        String participant1Username = uniqueUsername();
        Long participant1UserId = registerUser(
                participant1Username, "StrongPassword123!"
        );

        String participant2Username = uniqueUsername();
        Long participant2UserId = registerUser(
                participant2Username, "StrongPassword123!"
        );

        String participant1Id = addParticipant(
                gameId, managerToken, participant1UserId
        );

        String participant2Id = addParticipant(
                gameId, managerToken, participant2UserId
        );

        addBuyIn(gameId, participant1Id, managerToken, 1000);
        addBuyIn(gameId, participant2Id, managerToken, 1000);

        startGame(gameId, managerToken);

        //setRoundActive(gameId, managerToken, false);

        mockMvc.perform(post("/games/" + gameId + "/end")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().is4xxClientError());

        // Game should still be RUNNING.
        mockMvc.perform(get("/games/" + gameId)
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gameStatus").value("RUNNING"))
                .andExpect(jsonPath("$.roundActive").value(true));
    }

    @Test
    void managerCanCreateGameWithName() throws Exception {
        String payload = """
                {
                    "name": "Friday Night High Stakes",
                    "chipConversionRate": 2.5
                }
                """;

        String response = mockMvc.perform(post("/games")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Friday Night High Stakes"))
                .andExpect(jsonPath("$.chipConversionRate").value(2.5))
                .andReturn().getResponse().getContentAsString();
    }

    @Test
    void managerCanDeleteOpenGame() throws Exception {
        String gameId = createGame(managerToken);

        mockMvc.perform(delete("/games/" + gameId)
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/games/" + gameId)
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void managerCanAddParticipantByUsernameAndSearchUsers() throws Exception {
        String gameId = createGame(managerToken);

        String participantUsername = uniqueUsername();
        registerUser(participantUsername, "StrongPassword123!");

        // Search user
        mockMvc.perform(get("/users/search?query=" + participantUsername)
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value(participantUsername));

        // Add participant by username
        String addPayload = String.format("{\"username\":\"%s\"}", participantUsername);
        mockMvc.perform(post("/games/" + gameId + "/participants")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(addPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value(participantUsername));
    }
}
