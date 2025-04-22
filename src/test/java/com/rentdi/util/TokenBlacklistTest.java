package com.rentdi.util;

import com.auth0.jwt.interfaces.DecodedJWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TokenBlacklistTest {

    @InjectMocks
    private TokenBlacklist tokenBlacklist;

    @Mock
    private DecodedJWT decodedJWT;

    private String testToken = "test.jwt.token";

    @BeforeEach
    void setUp() {
        // Initialize tokenBlacklist
        tokenBlacklist = new TokenBlacklist();
    }

    @Test
    @DisplayName("Should add token to blacklist")
    void addToBlacklistShouldStoreToken() {
        // Arrange
        when(decodedJWT.getExpiresAt()).thenReturn(new Date(System.currentTimeMillis() + 3600000)); // 1 hour from now

        // Act
        tokenBlacklist.addToBlacklist(testToken, decodedJWT);

        // Assert
        assertTrue(tokenBlacklist.isBlacklisted(testToken));
    }

    @Test
    @DisplayName("Should check if token is blacklisted")
    void isBlacklistedShouldReturnTrueForBlacklistedToken() {
        // Arrange
        when(decodedJWT.getExpiresAt()).thenReturn(new Date(System.currentTimeMillis() + 3600000));
        tokenBlacklist.addToBlacklist(testToken, decodedJWT);

        // Act & Assert
        assertTrue(tokenBlacklist.isBlacklisted(testToken));
        assertFalse(tokenBlacklist.isBlacklisted("other.token"));
    }

    @Test
    @DisplayName("Should clean up expired tokens")
    void cleanupExpiredTokensShouldRemoveExpiredTokens() {
        // Arrange
        String expiredToken = "expired.token";
        String validToken = "valid.token";

        // Mock expired token (1 hour ago)
        DecodedJWT expiredJWT = org.mockito.Mockito.mock(DecodedJWT.class);
        when(expiredJWT.getExpiresAt()).thenReturn(new Date(System.currentTimeMillis() - 3600000));
        
        // Mock valid token (1 hour from now)
        when(decodedJWT.getExpiresAt()).thenReturn(new Date(System.currentTimeMillis() + 3600000));

        // Add both tokens to blacklist
        tokenBlacklist.addToBlacklist(expiredToken, expiredJWT);
        tokenBlacklist.addToBlacklist(validToken, decodedJWT);

        // Act
        tokenBlacklist.cleanupExpiredTokens();

        // Assert
        assertFalse(tokenBlacklist.isBlacklisted(expiredToken), "Expired token should be removed");
        assertTrue(tokenBlacklist.isBlacklisted(validToken), "Valid token should remain");
    }
} 