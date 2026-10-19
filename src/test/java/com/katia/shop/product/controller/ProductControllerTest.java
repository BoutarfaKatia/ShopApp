package com.katia.shop.product.controller;

import com.katia.shop.auth.security.JwtService;
import com.katia.shop.auth.security.RestAccessDeniedHandler;
import com.katia.shop.auth.security.RestAuthenticationEntryPoint;
import com.katia.shop.auth.security.SecurityConfig;
import com.katia.shop.product.dto.ProductResponse;
import com.katia.shop.product.service.ProductService;
import com.katia.shop.auth.service.CustomUserDetailsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import java.math.BigDecimal;
import java.time.Instant;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test")
@WebMvcTest(ProductController.class)
@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class})
class ProductControllerTest {

    private static final String VALID_BODY = """
            {"name":"Test Product","description":"Product for testing","price":100.00,"stock":10}
            """;

    @Autowired private MockMvc mockMvc;
    @MockitoBean private ProductService productService;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private CustomUserDetailsService customUserDetailsService;

    @Test
    void createProduct_shouldReturn401WhenAnonymous() throws Exception {
        mockMvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "ORDER_READ")
    void createProduct_shouldReturn403WhenMissingProductWritePermission() throws Exception {
        mockMvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "PRODUCT_WRITE")
    void createProduct_shouldReturn201WithLocationWhenAuthorized() throws Exception {
        when(productService.createProduct(any())).thenReturn(new ProductResponse(
                1L, "Test Product", "Product for testing",
                new BigDecimal("100.00"), 10, Instant.now(), true));

        mockMvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/products/1"))
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @WithMockUser(authorities = "PRODUCT_WRITE")
    void createProduct_shouldReturn400WhenBodyIsInvalid() throws Exception {
        String invalid = """
                {"name":"","description":"x","price":-5,"stock":10}
                """;

        mockMvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON).content(invalid))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.name").exists())
                .andExpect(jsonPath("$.fieldErrors.price").exists());
        verifyNoInteractions(productService);
    }

    @Test
    void getProducts_shouldBePublic() throws Exception {
        when(productService.getProducts(any(), any())).thenReturn(Page.empty());

        mockMvc.perform(get("/api/products")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "PRODUCT_DELETE")
    void deleteProduct_shouldReturn204() throws Exception {
        mockMvc.perform(delete("/api/products/5")).andExpect(status().isNoContent());
        verify(productService).deleteProduct(5L);
    }
}