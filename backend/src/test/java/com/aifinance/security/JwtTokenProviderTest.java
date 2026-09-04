package com.aifinance.security;

import com.aifinance.entity.Role;
import com.aifinance.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    private JwtTokenProvider tokenProvider;
    private User testUser;

    @BeforeEach
    void setUp() {
        String secret = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
        tokenProvider = new JwtTokenProvider(secret, 3600000, 86400000);

        testUser = new User("testanalyst", "test@finance.com", "hash", Role.ROLE_ANALYST);
        testUser.setId(42L);
    }

    @Test
    void testGenerateAndValidateToken() {
        String token = tokenProvider.generateAccessToken(testUser);
        assertNotNull(token);
        assertTrue(tokenProvider.validateToken(token));

        String username = tokenProvider.getUsernameFromToken(token);
        assertEquals("testanalyst", username);

        String role = tokenProvider.getRoleFromToken(token);
        assertEquals("ROLE_ANALYST", role);
    }

    @Test
    void testInvalidToken() {
        assertFalse(tokenProvider.validateToken("invalid.token.here"));
    }
}
