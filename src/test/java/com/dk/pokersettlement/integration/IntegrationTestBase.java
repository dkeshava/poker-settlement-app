package com.dk.pokersettlement.integration;

//import com.fasterxml.jackson.databind.JsonNode;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public abstract class IntegrationTestBase {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    protected String managerUsername;
    protected String managerPassword;
    protected String managerToken;
    protected Long managerId;

    @BeforeEach
    void initializeManager() throws Exception {
        managerUsername = uniqueUsername();
        managerPassword = "StrongPassword123!";

        managerId = registerUser(managerUsername, managerPassword);
        managerToken = loginUser(managerUsername, managerPassword);
    }

    protected String uniqueUsername() {
        return "test_" + UUID.randomUUID().toString().replace("-", "");
    }

    protected Long registerUser(String username, String password)
            throws Exception {

        String body = """
                {
                    "username": "%s",
                    "password": "%s"
                }
                """.formatted(username, password);

        MvcResult result = mockMvc.perform(post("/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode response = objectMapper.readTree(
                result.getResponse().getContentAsString()
        );

        return response.get("id").asLong();
    }

    protected String loginUser(String username, String password)
            throws Exception {

        String body = """
                {
                    "username": "%s",
                    "password": "%s"
                }
                """.formatted(username, password);

        return mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    protected String createGame(String token) throws Exception {

        String body = """
                {
                    "chipConversionRate": 0.10
                }
                """;

        MvcResult result = mockMvc.perform(post("/games")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode response = objectMapper.readTree(
                result.getResponse().getContentAsString()
        );

        return response.get("id").asText();
    }

    protected String addParticipant(
            String gameId,
            String token,
            Long userId
    ) throws Exception {

        String body = """
                {
                    "userId": %d
                }
                """.formatted(userId);

        MvcResult result = mockMvc.perform(
                        post("/games/" + gameId + "/participants")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode response = objectMapper.readTree(
                result.getResponse().getContentAsString()
        );

        return response.get("participantId").asText();
    }

    protected String addBuyIn(
            String gameId,
            String participantId,
            String token,
            long chips
    ) throws Exception {

        String body = """
                {
                    "chipValue": %d
                }
                """.formatted(chips);

        MvcResult result = mockMvc.perform(
                        post("/games/" + gameId
                                + "/participants/" + participantId + "/")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode response = objectMapper.readTree(
                result.getResponse().getContentAsString()
        );

        return response.get("buyInId").asText();
    }

    protected void startGame(String gameId, String token)
            throws Exception {

        mockMvc.perform(post("/games/" + gameId + "/start")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    protected void endGame(String gameId, String token)
            throws Exception {

        mockMvc.perform(post("/games/" + gameId + "/end")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    protected void setRoundActive(
            String gameId,
            String token,
            boolean active
    ) throws Exception {

        String body = """
                {
                    "roundActive": %s
                }
                """.formatted(active);

        mockMvc.perform(patch("/games/" + gameId + "/round-state")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
    }
    protected record FinalChipEntry(Long participantId, long chips) {}

    protected void submitFinalChips(
            String gameId,
            String token,
            FinalChipEntry... entries
    ) throws Exception {

        List<Map<String, Object>> finalChipValues = Arrays.stream(entries)
                .map(entry -> Map.<String, Object>of(
                        "participantId", entry.participantId(),
                        "chipValue", entry.chips()
                ))
                .toList();

        String body = objectMapper.writeValueAsString(
                Map.of("finalChipValues", finalChipValues)
        );

        mockMvc.perform(put("/games/" + gameId + "/final-chip-values")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
    }

    protected String registerAndLoginParticipant() throws Exception {
        String username = uniqueUsername();
        registerUser(username, "StrongPassword123!");
        return loginUser(username, "StrongPassword123!");
    }
}

