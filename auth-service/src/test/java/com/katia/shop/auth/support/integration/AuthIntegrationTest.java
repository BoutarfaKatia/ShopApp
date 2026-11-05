package com.katia.shop.auth.support.integration;


import com.katia.shop.auth.dto.LoginRequest;
import com.katia.shop.auth.dto.RegisterRequest;
import com.katia.shop.auth.support.IntegrationTest;
import com.katia.shop.user.entity.Role;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.ObjectMapper;
import com.katia.shop.user.entity.User;
import com.katia.shop.user.repository.UserRepository;
import org.springframework.http.MediaType;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@IntegrationTest
class AuthIntegrationTest {

    private static final String PASSWORD = "Password123!";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private record Session(String accessToken, Cookie refreshCookie) {}

    // ---------- register ----------

    @Test
    void register_shouldCreateUserWithHashedPasswordAndUserRole() throws Exception {
        String email = uniqueEmail();

        register("Test User", email).andExpect(status().isCreated());

        User saved = userRepository.findByEmail(email).orElseThrow();
        assertEquals(Role.USER, saved.getRole());
        assertNotEquals(PASSWORD, saved.getPasswordHash());
        assertTrue(passwordEncoder.matches(PASSWORD, saved.getPasswordHash()));
    }

    @Test
    void register_shouldIgnoreRoleSentByClient() throws Exception {
        String email = uniqueEmail();
        String body = """
                {"fullName":"Hacker","email":"%s","password":"%s","role":"ADMIN"}
                """.formatted(email, PASSWORD);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());

        assertEquals(Role.USER, userRepository.findByEmail(email).orElseThrow().getRole());
    }

    @Test
    void register_shouldNormalizeEmailCase() throws Exception {
        String email = uniqueEmail();

        register("Test User", email.toUpperCase()).andExpect(status().isCreated());

        assertTrue(userRepository.findByEmail(email).isPresent());
        login(email.toUpperCase(), PASSWORD).andExpect(status().isOk());
    }

    @Test
    void register_shouldReturn409WhenEmailAlreadyExists() throws Exception {
        String email = uniqueEmail();
        register("Test User", email).andExpect(status().isCreated());

        register("Test User", email).andExpect(status().isConflict());
    }

    @Test
    void register_shouldReturn400WhenDataIsInvalid() throws Exception {
        register("", uniqueEmail())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors.fullName").exists());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterRequest("Test User", uniqueEmail(), "short"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.password").exists());
    }

    // ---------- login ----------

    @Test
    void login_shouldReturnAccessTokenAndSecureRefreshCookie() throws Exception {
        String email = uniqueEmail();
        register("Test User", email).andExpect(status().isCreated());

        login(email, PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(cookie().exists("refreshToken"))
                .andExpect(cookie().httpOnly("refreshToken", true))
                .andExpect(cookie().secure("refreshToken", true))
                .andExpect(cookie().path("refreshToken", "/api/auth"))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("SameSite=Strict")));
    }

    @Test
    void login_shouldReturn401WithSameMessageForWrongPasswordAndUnknownEmail() throws Exception {
        String email = uniqueEmail();
        register("Test User", email).andExpect(status().isCreated());

        login(email, "WrongPassword123!")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));

        login(uniqueEmail(), PASSWORD)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    // ---------- secure the endpoints ----------



    // ---------- refresh / logout ----------

    @Test
    void refresh_shouldRotateTokens() throws Exception {
        Session session = registerAndLogin();

        MvcResult result = mockMvc.perform(post("/api/auth/refresh").cookie(session.refreshCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andReturn();

        Cookie rotated = result.getResponse().getCookie("refreshToken");
        assertNotNull(rotated);
        assertNotEquals(session.refreshCookie().getValue(), rotated.getValue());
    }

    @Test
    void refresh_shouldRevokeAllSessionsWhenTokenIsReused() throws Exception {
        Session session = registerAndLogin();
        MvcResult first = mockMvc.perform(post("/api/auth/refresh").cookie(session.refreshCookie()))
                .andExpect(status().isOk()).andReturn();
        Cookie rotated = first.getResponse().getCookie("refreshToken");

        // reuse the old token
        mockMvc.perform(post("/api/auth/refresh").cookie(session.refreshCookie()))
                .andExpect(status().isUnauthorized());
        // reuse detection:
        mockMvc.perform(post("/api/auth/refresh").cookie(rotated))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refresh_shouldReturn401WithoutCookie() throws Exception {
        mockMvc.perform(post("/api/auth/refresh")).andExpect(status().isUnauthorized());
    }

    @Test
    void refresh_shouldReturn401AfterLogout() throws Exception {
        Session session = registerAndLogin();

        mockMvc.perform(post("/api/auth/logout").cookie(session.refreshCookie()))
                .andExpect(status().isNoContent());
        mockMvc.perform(post("/api/auth/refresh").cookie(session.refreshCookie()))
                .andExpect(status().isUnauthorized());
    }

    // ----------helping methods ----------

    private ResultActions register(String fullName, String email) throws Exception {
        return mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new RegisterRequest(fullName, email, PASSWORD))));
    }

    private ResultActions login(String email, String password) throws Exception {
        return mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest(email, password))));
    }

    private Session registerAndLogin() throws Exception {
        String email = uniqueEmail();
        register("Test User", email).andExpect(status().isCreated());
        MvcResult result = login(email, PASSWORD).andExpect(status().isOk()).andReturn();
        String accessToken = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("accessToken").asText();
        return new Session(accessToken, result.getResponse().getCookie("refreshToken"));
    }


    private static String uniqueEmail() { return "user-" + UUID.randomUUID() + "@example.com"; }
}