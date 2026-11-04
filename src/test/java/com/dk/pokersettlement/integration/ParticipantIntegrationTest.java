package com.dk.pokersettlement.integration;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ParticipantIntegrationTest extends IntegrationTestBase {

    @Test
    void managerCanAddParticipant() throws Exception {

        String gameId = createGame(managerToken);

        String username = uniqueUsername();
        Long userId = registerUser(username, "StrongPassword123!");

        mockMvc.perform(post("/games/" + gameId + "/participants")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "userId": %d
                                }
                                """.formatted(userId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.gameId").value(gameId))
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.username").value(username));
    }

    @Test
    void managerCanListParticipants() throws Exception {

        String gameId = createGame(managerToken);

        Long userId = registerUser(uniqueUsername(), "StrongPassword123!");
        addParticipant(gameId, managerToken, userId);

        mockMvc.perform(get("/games/" + gameId + "/participants")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void participantCanListParticipants() throws Exception {

        String gameId = createGame(managerToken);

        String username = uniqueUsername();
        Long userId = registerUser(username, "StrongPassword123!");

        String participantToken = loginUser(
                username, "StrongPassword123!"
        );

        addParticipant(gameId, managerToken, userId);

        mockMvc.perform(get("/games/" + gameId + "/participants")
                        .header("Authorization", "Bearer " + participantToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void outsiderCannotListParticipants() throws Exception {

        String gameId = createGame(managerToken);
        String outsiderToken = registerAndLoginParticipant();

        mockMvc.perform(get("/games/" + gameId + "/participants")
                        .header("Authorization", "Bearer " + outsiderToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void duplicateParticipantIsRejected() throws Exception {

        String gameId = createGame(managerToken);

        Long userId = registerUser(uniqueUsername(), "StrongPassword123!");

        addParticipant(gameId, managerToken, userId);

        mockMvc.perform(post("/games/" + gameId + "/participants")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "userId": %d
                                }
                                """.formatted(userId)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void nonManagerCannotAddParticipant() throws Exception {

        String gameId = createGame(managerToken);

        String outsiderToken = registerAndLoginParticipant();

        Long userId = registerUser(uniqueUsername(), "StrongPassword123!");

        mockMvc.perform(post("/games/" + gameId + "/participants")
                        .header("Authorization", "Bearer " + outsiderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "userId": %d
                                }
                                """.formatted(userId)))
                .andExpect(status().isForbidden());
    }

    @Test
    void managerCanDeleteParticipantWithoutBuyIns() throws Exception {

        String gameId = createGame(managerToken);

        Long userId = registerUser(uniqueUsername(), "StrongPassword123!");
        String participantId = addParticipant(gameId, managerToken, userId);

        mockMvc.perform(post("/games/" + gameId
                        + "/participants/" + participantId)
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.participantId").value(participantId))
                .andExpect(jsonPath("$.userId").value(userId));
    }

    @Test
    void participantWithBuyInsCannotBeDeleted() throws Exception {

        String gameId = createGame(managerToken);

        Long userId = registerUser(uniqueUsername(), "StrongPassword123!");
        String participantId = addParticipant(gameId, managerToken, userId);

        addBuyIn(gameId, participantId, managerToken, 1000);

        mockMvc.perform(post("/games/" + gameId
                        + "/participants/" + participantId)
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void nonManagerCannotDeleteParticipant() throws Exception {

        String gameId = createGame(managerToken);

        Long userId = registerUser(uniqueUsername(), "StrongPassword123!");
        String participantId = addParticipant(gameId, managerToken, userId);

        String outsiderToken = registerAndLoginParticipant();

        mockMvc.perform(post("/games/" + gameId
                        + "/participants/" + participantId)
                        .header("Authorization", "Bearer " + outsiderToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void participantCannotBeAddedDuringActiveRound() throws Exception {

        String gameId = createGame(managerToken);

        // Participant 1
        Long existingUserId1 = registerUser(
                uniqueUsername(), "StrongPassword123!"
        );

        String participantId1 = addParticipant(
                gameId, managerToken, existingUserId1
        );

        addBuyIn(gameId, participantId1, managerToken, 1000);

        // Participant 2
        Long existingUserId2 = registerUser(
                uniqueUsername(), "StrongPassword123!"
        );

        String participantId2 = addParticipant(
                gameId, managerToken, existingUserId2
        );

        addBuyIn(gameId, participantId2, managerToken, 1000);

        // Start game (round becomes active automatically)
        startGame(gameId, managerToken);

        // New participant attempting to join during active round
        Long newUserId = registerUser(
                uniqueUsername(), "StrongPassword123!"
        );

        mockMvc.perform(post("/games/" + gameId + "/participants")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "userId": %d
                            }
                            """.formatted(newUserId)))
                .andExpect(status().is4xxClientError());
    }
}
