package com.katia.shop.auth.support.integration;

import tools.jackson.databind.json.JsonMapper;
import com.katia.shop.auth.dto.LoginRequest;
import com.katia.shop.auth.dto.RegisterRequest;
import com.katia.shop.auth.support.IntegrationTest;
import com.katia.shop.user.repository.UserRepository;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.test.web.servlet.MockMvc;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class JwtContractIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JsonMapper jsonMapper;
    @Autowired private UserRepository userRepository;

    @Test
    void accessToken_shouldBeVerifiableWithPublishedPublicKey() throws Exception {
        String email = "user-" + UUID.randomUUID() + "@example.com";
        String password = "Password123!";

        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(new RegisterRequest("Test User", email, password))))
                .andExpect(status().isCreated());
        String body = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(new LoginRequest(email, password))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String accessToken = jsonMapper.readTree(body).get("accessToken").asText();

        String jwksJson = mockMvc.perform(get("/.well-known/jwks.json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.keys.length()").value(1))
                .andExpect(jsonPath("$.keys[0].kty").value("RSA"))
                .andExpect(jsonPath("$.keys[0].d").doesNotExist())
                .andReturn().getResponse().getContentAsString();

        RSAKey publicKey = (RSAKey) JWKSet.parse(jwksJson).getKeys().get(0);
        Jwt jwt = NimbusJwtDecoder.withPublicKey(publicKey.toRSAPublicKey()).build().decode(accessToken);

        Long userId = userRepository.findByEmail(email).orElseThrow().getId();
        assertThat(jwt.getSubject()).isEqualTo(String.valueOf(userId));
        assertThat(jwt.getClaimAsString("iss")).isEqualTo("http://auth-service");
        assertThat(jwt.getClaimAsString("email")).isEqualTo(email);
        assertThat(jwt.getClaimAsStringList("authorities")).contains("ROLE_USER", "ORDER_WRITE");
        assertThat(jwt.getHeaders()).containsEntry("alg", "RS256").containsEntry("kid", publicKey.getKeyID());
    }
}