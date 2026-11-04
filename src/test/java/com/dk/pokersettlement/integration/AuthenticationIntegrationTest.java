package com.dk.pokersettlement.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthenticationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private String uniqueUsername() {
        return "integration_" + UUID.randomUUID();
    }

    private String registerUser(String username, String password)
            throws Exception {

        String requestBody = """
                {
                    "username": "%s",
                    "password": "%s"
                }
                """.formatted(username, password);

        mockMvc.perform(post("/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk());

        return username;
    }

    private String loginUser(String username, String password)
            throws Exception {

        String requestBody = """
                {
                    "username": "%s",
                    "password": "%s"
                }
                """.formatted(username, password);

        return mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    @Test
    void registerAndLoginSuccessfully() throws Exception {

        String username = uniqueUsername();
        String password = "StrongPassword123!";

        registerUser(username, password);

        String token = loginUser(username, password);

        org.junit.jupiter.api.Assertions.assertNotNull(token);
        org.junit.jupiter.api.Assertions.assertFalse(token.isBlank());
    }

    @Test
    void loginWithIncorrectPasswordShouldFail() throws Exception {

        String username = uniqueUsername();
        String password = "StrongPassword123!";

        registerUser(username, password);

        String requestBody = """
                {
                    "username": "%s",
                    "password": "WrongPassword123!"
                }
                """.formatted(username);

        mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void requestWithoutTokenShouldReturn401() throws Exception {

        mockMvc.perform(get("/games/999999"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void requestWithInvalidTokenShouldReturn401() throws Exception {

        mockMvc.perform(get("/games/999999")
                        .header(
                                "Authorization",
                                "Bearer invalid.jwt.token"
                        ))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void validTokenShouldPassAuthentication() throws Exception {

        String username = uniqueUsername();
        String password = "StrongPassword123!";

        registerUser(username, password);

        String token = loginUser(username, password);

        /*
         * Game 999999 should not exist.
         *
         * If authentication succeeds, the request should reach
         * the application and fail because the game is missing.
         *
         * We expect 404, not 401.
         */
        mockMvc.perform(get("/games/999999")
                        .header(
                                "Authorization",
                                "Bearer " + token
                        ))
                .andExpect(status().isNotFound());
    }
}