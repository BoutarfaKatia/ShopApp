package com.katia.shop.product.integration;

import com.katia.shop.product.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
public class ProductApiIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    private static String body() {
        return """
                {"name":"Keyboard %s","description":"Mechanical keyboard","price":99.90,"stock":5}
                """.formatted(UUID.randomUUID());
    }

    @Test
    void createProduct_shouldReturn201WithProductWriteAuthority() throws Exception {
        mockMvc.perform(post("/api/products")
                        .with(jwt().authorities(
                                new SimpleGrantedAuthority("PRODUCT_WRITE")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body()))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber());
    }

    @Test
    void createProduct_shouldReturn403WithoutPermission() throws Exception {
        mockMvc.perform(post("/api/products")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ORDER_READ")))
                        .contentType(MediaType.APPLICATION_JSON).content(body()))
                .andExpect(status().isForbidden());
    }

    @Test
    void createProduct_shouldReturn401WithoutToken() throws Exception {
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON).content(body()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getProducts_shouldBePublic() throws Exception {
        mockMvc.perform(get("/api/products")).andExpect(status().isOk());
    }

    @Test
    void getProducts_shouldReturn401WhenBearerIsNotAJwt() throws Exception {
        mockMvc.perform(get("/api/products").header(HttpHeaders.AUTHORIZATION, "Bearer not-a-jwt"))
                .andExpect(status().isUnauthorized());
    }
}
