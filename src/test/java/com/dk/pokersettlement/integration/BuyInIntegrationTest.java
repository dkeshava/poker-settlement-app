package com.dk.pokersettlement.integration;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class BuyInIntegrationTest extends IntegrationTestBase {

    private String createGameWithParticipant() throws Exception {

        String gameId = createGame(managerToken);

        Long userId = registerUser(
                uniqueUsername(), "StrongPassword123!"
        );

        return gameId + ":" + addParticipant(
                gameId, managerToken, userId
        );
    }
    private String createGameWithTwoParticipants() throws Exception {

        String gameId = createGame(managerToken);

        Long userId1 = registerUser(
                uniqueUsername(), "StrongPassword123!"
        );

        Long userId2 = registerUser(
                uniqueUsername(), "StrongPassword123!"
        );

        String participantId1 = addParticipant(
                gameId, managerToken, userId1
        );

        String participantId2 = addParticipant(
                gameId, managerToken, userId2
        );

        return gameId + ":" + participantId1 + ":" + participantId2;
    }

    @Test
    void managerCanAddBuyIn() throws Exception {

        String[] data = createGameWithParticipant().split(":");

        mockMvc.perform(post("/games/" + data[0]
                        + "/participants/" + data[1] + "/")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "chipValue": 1000
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.participantId").value(data[1]))
                .andExpect(jsonPath("$.chipValue").value(1000));
    }

    @Test
    void participantCanHaveMultipleBuyIns() throws Exception {

        String[] data = createGameWithParticipant().split(":");

        addBuyIn(data[0], data[1], managerToken, 1000);
        addBuyIn(data[0], data[1], managerToken, 500);

        // Both buy-ins must be independently persisted.
        // The IDs must also be different.
        String firstBuyIn = addBuyIn(
                data[0], data[1], managerToken, 100
        );

        String secondBuyIn = addBuyIn(
                data[0], data[1], managerToken, 200
        );

        org.junit.jupiter.api.Assertions.assertNotEquals(
                firstBuyIn, secondBuyIn
        );
    }

    @Test
    void managerCanUpdateBuyIn() throws Exception {

        String[] data = createGameWithParticipant().split(":");

        String buyInId = addBuyIn(
                data[0], data[1], managerToken, 1000
        );

        mockMvc.perform(patch("/games/" + data[0]
                        + "/buy-ins/" + buyInId)
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "chipValue": 1500
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.buyInId").value(buyInId))
                .andExpect(jsonPath("$.chipValue").value(1500));
    }

    @Test
    void managerCanDeleteBuyIn() throws Exception {

        String[] data = createGameWithParticipant().split(":");

        String buyInId = addBuyIn(
                data[0], data[1], managerToken, 1000
        );

        mockMvc.perform(delete("/games/" + data[0]
                        + "/buy-ins/" + buyInId)
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isNoContent());

        // Deleting the same buy-in again should fail.
        mockMvc.perform(delete("/games/" + data[0]
                        + "/buy-ins/" + buyInId)
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void zeroBuyInIsRejected() throws Exception {

        String[] data = createGameWithParticipant().split(":");

        mockMvc.perform(post("/games/" + data[0]
                        + "/participants/" + data[1] + "/")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "chipValue": 0
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void negativeBuyInIsRejected() throws Exception {

        String[] data = createGameWithParticipant().split(":");

        mockMvc.perform(post("/games/" + data[0]
                        + "/participants/" + data[1] + "/")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "chipValue": -100
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void nullBuyInIsRejected() throws Exception {

        String[] data = createGameWithParticipant().split(":");

        mockMvc.perform(post("/games/" + data[0]
                        + "/participants/" + data[1] + "/")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "chipValue": null
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void outsiderCannotAddBuyIn() throws Exception {

        String[] data = createGameWithParticipant().split(":");
        String outsiderToken = registerAndLoginParticipant();

        mockMvc.perform(post("/games/" + data[0]
                        + "/participants/" + data[1] + "/")
                        .header("Authorization", "Bearer " + outsiderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "chipValue": 1000
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void buyInCannotBeAddedDuringActiveRound() throws Exception {

        String[] data = createGameWithParticipant().split(":");

        // Add buy-in for the first participant
        addBuyIn(data[0], data[1], managerToken, 1000);

        // Add a second participant
        Long userId = registerUser(uniqueUsername(), "StrongPassword123!");
        String participantId2 = addParticipant(data[0], managerToken, userId);

        // Add buy-in for the second participant
        addBuyIn(data[0], participantId2, managerToken, 1000);

        // Starting the game automatically activates the round
        startGame(data[0], managerToken);

        // Try adding another buy-in during the active round
        mockMvc.perform(post("/games/" + data[0]
                        + "/participants/" + data[1])
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "chipValue": 500
                            }
                            """))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void buyInCannotBeModifiedAfterGameEnds() throws Exception {

        String[] data = createGameWithTwoParticipants().split(":");

        String gameId = data[0];
        String participantId1 = data[1];
        String participantId2 = data[2];

        String buyInId = addBuyIn(
                gameId, participantId1, managerToken, 1000
        );

// Give second participant a buy-in too
        addBuyIn(
                gameId, participantId2, managerToken, 1000
        );

        startGame(gameId, managerToken);
        // Stop the active round
        setRoundActive(gameId, managerToken,false);
        endGame(gameId, managerToken);

        mockMvc.perform(patch("/games/" + data[0]
                        + "/buy-ins/" + buyInId)
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "chipValue": 1500
                            }
                            """))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void buyInFromAnotherGameCannotBeUpdated() throws Exception {

        String[] firstGame = createGameWithParticipant().split(":");
        String[] secondGame = createGameWithParticipant().split(":");

        String buyInId = addBuyIn(
                firstGame[0], firstGame[1], managerToken, 1000
        );

        mockMvc.perform(patch("/games/" + secondGame[0]
                        + "/buy-ins/" + buyInId)
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "chipValue": 1500
                                }
                                """))
                .andExpect(status().isNotFound());
    }
}

