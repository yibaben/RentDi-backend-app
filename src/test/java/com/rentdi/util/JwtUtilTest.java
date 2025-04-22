package com.rentdi.util;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.rentdi.entity.BaseEntity;
import com.rentdi.entity.Role;
import com.rentdi.entity.User;
import com.rentdi.entity.enums.RoleName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class JwtUtilTest {

    @InjectMocks
    private JwtUtil jwtUtil;

    private User testUser;
    private String testSecret = "testSecretKeyThatIsLongEnoughForHmac512Algorithm";
    private long testExpiration = 3600000; // 1 hour

    @BeforeEach
    void setUp() {
        // Set JWT properties
        ReflectionTestUtils.setField(jwtUtil, "secret", testSecret);
        ReflectionTestUtils.setField(jwtUtil, "expiration", testExpiration);

        // Create test roles
        Set<Role> roles = new HashSet<>();
        Role role = Role.builder()
                .name(RoleName.TENANT)
                .build();
        roles.add(role);

        // Create test user
        testUser = User.builder()
                .email("test@example.com")
                .firstName("Test")
                .lastName("User")
                .roles(roles)
                .build();
        ReflectionTestUtils.setField(testUser, "id", 1L);
    }

    @Test
    @DisplayName("Should generate JWT token with correct claims")
    void generateTokenShouldCreateValidJwtWithClaims() {
        // Act
        String token = jwtUtil.generateToken(testUser);

        // Assert
        assertNotNull(token);
        assertTrue(token.length() > 20);
        
        DecodedJWT decodedJwt = jwtUtil.verifyToken(token);
        assertEquals(testUser.getEmail(), decodedJwt.getSubject());
        assertEquals(testUser.getId().longValue(), decodedJwt.getClaim("userId").asLong());
        assertEquals(testUser.getEmail(), decodedJwt.getClaim("email").asString());
        assertEquals(testUser.getFirstName(), decodedJwt.getClaim("firstName").asString());
        assertEquals(testUser.getLastName(), decodedJwt.getClaim("lastName").asString());
        
        // Verify roles
        String roles = decodedJwt.getClaim("roles").asString();
        assertEquals("ROLE_TENANT", roles);
        
        // Verify expiration
        Date expiresAt = decodedJwt.getExpiresAt();
        assertTrue(expiresAt.after(new Date()));
    }

    @Test
    @DisplayName("Should extract username from token")
    void getUsernameFromTokenShouldReturnCorrectUsername() {
        // Arrange
        String token = jwtUtil.generateToken(testUser);

        // Act
        String username = jwtUtil.getUsernameFromToken(token);

        // Assert
        assertEquals(testUser.getEmail(), username);
    }

    @Test
    @DisplayName("Should extract user ID from token")
    void getUserIdFromTokenShouldReturnCorrectUserId() {
        // Arrange
        String token = jwtUtil.generateToken(testUser);

        // Act
        Long userId = jwtUtil.getUserIdFromToken(token);

        // Assert
        assertEquals(testUser.getId(), userId);
    }

    @Test
    @DisplayName("Should verify valid token")
    void verifyTokenShouldReturnDecodedJwtForValidToken() {
        // Arrange
        String token = jwtUtil.generateToken(testUser);

        // Act
        DecodedJWT decodedJWT = jwtUtil.verifyToken(token);

        // Assert
        assertNotNull(decodedJWT);
        assertEquals(testUser.getEmail(), decodedJWT.getSubject());
    }

    @Test
    @DisplayName("Should validate token")
    void validateTokenShouldReturnTrueForValidToken() {
        // Arrange
        String token = jwtUtil.generateToken(testUser);

        // Act
        boolean isValid = jwtUtil.validateToken(token);

        // Assert
        assertTrue(isValid);
    }

    @Test
    @DisplayName("Should return false for invalid token")
    void validateTokenShouldReturnFalseForInvalidToken() {
        // Arrange
        String invalidToken = "invalid.token.string";

        // Act
        boolean isValid = jwtUtil.validateToken(invalidToken);

        // Assert
        assertFalse(isValid);
    }

    private static <T extends BaseEntity> T setId(T entity, Long id) {
        ReflectionTestUtils.setField(entity, "id", id);
        return entity;
    }
} 