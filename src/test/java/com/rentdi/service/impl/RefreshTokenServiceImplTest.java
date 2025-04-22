package com.rentdi.service.impl;

import com.rentdi.Repository.RefreshTokenRepository;
import com.rentdi.Repository.UserRepository;
import com.rentdi.entity.RefreshToken;
import com.rentdi.entity.User;
import com.rentdi.exception.ResourceNotFoundException;
import com.rentdi.exception.TokenRefreshException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.rentdi.entity.BaseEntity;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceImplTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private RefreshTokenServiceImpl refreshTokenService;

    private User testUser;
    private RefreshToken testRefreshToken;
    private final Long refreshTokenDurationMs = 2592000000L; // 30 days

    @BeforeEach
    void setUp() {
        // Set refresh token duration
        ReflectionTestUtils.setField(refreshTokenService, "refreshTokenDurationMs", refreshTokenDurationMs);

        // Create test user
        testUser = User.builder()
                .email("test@example.com")
                .build();
        ReflectionTestUtils.setField(testUser, "id", 1L);

        // Create test refresh token
        testRefreshToken = RefreshToken.builder()
                .token(UUID.randomUUID().toString())
                .expiryDate(Instant.now().plusMillis(refreshTokenDurationMs))
                .build();
        ReflectionTestUtils.setField(testRefreshToken, "id", 1L);
    }

    @Test
    @DisplayName("Should find refresh token by token string")
    void findByTokenShouldReturnRefreshToken() {
        // Arrange
        String tokenString = testRefreshToken.getToken();
        when(refreshTokenRepository.findByToken(tokenString)).thenReturn(Optional.of(testRefreshToken));

        // Act
        Optional<RefreshToken> result = refreshTokenService.findByToken(tokenString);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(testRefreshToken, result.get());
        verify(refreshTokenRepository).findByToken(tokenString);
    }

    @Test
    @DisplayName("Should return empty optional when token is not found")
    void findByTokenShouldReturnEmptyOptionalWhenNotFound() {
        // Arrange
        String tokenString = "non-existent-token";
        when(refreshTokenRepository.findByToken(tokenString)).thenReturn(Optional.empty());

        // Act
        Optional<RefreshToken> result = refreshTokenService.findByToken(tokenString);

        // Assert
        assertFalse(result.isPresent());
        verify(refreshTokenRepository).findByToken(tokenString);
    }

    @Test
    @DisplayName("Should create new refresh token when user doesn't have one")
    void createRefreshTokenShouldCreateNewTokenWhenNoneExists() {
        // Arrange
        when(refreshTokenRepository.findByUser(testUser)).thenReturn(Optional.empty());
        
        // Important: Make sure to capture what's being saved and return testRefreshToken
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> {
            // Get the RefreshToken that's being saved
            RefreshToken tokenBeingSaved = invocation.getArgument(0);
            // Ensure our testRefreshToken has the same user
            ReflectionTestUtils.setField(testRefreshToken, "user", tokenBeingSaved.getUser());
            return testRefreshToken;
        });

        // Act
        RefreshToken result = refreshTokenService.createRefreshToken(testUser);

        // Assert
        assertNotNull(result);
        assertEquals(testRefreshToken, result);
        assertEquals(testUser, result.getUser());
        verify(refreshTokenRepository).findByUser(testUser);
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("Should update existing refresh token when user already has one")
    void createRefreshTokenShouldUpdateExistingToken() {
        // Arrange
        when(refreshTokenRepository.findByUser(testUser)).thenReturn(Optional.of(testRefreshToken));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(testRefreshToken);

        // Act
        RefreshToken result = refreshTokenService.createRefreshToken(testUser);

        // Assert
        assertNotNull(result);
        assertEquals(testRefreshToken, result);
        verify(refreshTokenRepository).findByUser(testUser);
        verify(refreshTokenRepository).save(testRefreshToken);
    }

    @Test
    @DisplayName("Should handle race condition when creating refresh token")
    void createRefreshTokenShouldHandleRaceCondition() {
        // Arrange
        when(refreshTokenRepository.findByUser(testUser)).thenReturn(Optional.empty());
        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenThrow(DataIntegrityViolationException.class)
                .thenReturn(testRefreshToken);
        
        // Mock handleRaceCondition method since it's called in a new transaction
        RefreshTokenServiceImpl spyService = spy(refreshTokenService);
        doReturn(testRefreshToken).when(spyService).handleRaceCondition(testUser);

        // Act
        RefreshToken result = spyService.createRefreshToken(testUser);

        // Assert
        assertNotNull(result);
        assertEquals(testRefreshToken, result);
        verify(refreshTokenRepository).findByUser(testUser);
        verify(refreshTokenRepository).save(any(RefreshToken.class));
        verify(spyService).handleRaceCondition(testUser);
    }

    @Test
    @DisplayName("Should handle race condition by finding and updating existing token")
    void handleRaceConditionShouldFindAndUpdateExistingToken() {
        // Arrange
        when(refreshTokenRepository.findByUser(testUser)).thenReturn(Optional.of(testRefreshToken));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(testRefreshToken);

        // Act
        RefreshToken result = refreshTokenService.handleRaceCondition(testUser);

        // Assert
        assertNotNull(result);
        assertEquals(testRefreshToken, result);
        verify(refreshTokenRepository).findByUser(testUser);
        verify(refreshTokenRepository).save(testRefreshToken);
    }

    @Test
    @DisplayName("Should handle race condition by cleaning up and creating new token")
    void handleRaceConditionShouldCleanupAndCreateNewToken() {
        // Arrange
        when(refreshTokenRepository.findByUser(testUser)).thenReturn(Optional.empty());
        doNothing().when(refreshTokenRepository).deleteByUser(testUser);
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(testRefreshToken);

        // Act
        RefreshToken result = refreshTokenService.handleRaceCondition(testUser);

        // Assert
        assertNotNull(result);
        assertEquals(testRefreshToken, result);
        verify(refreshTokenRepository).findByUser(testUser);
        verify(refreshTokenRepository).deleteByUser(testUser);
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("Should verify and update token expiration when token is valid")
    void verifyExpirationShouldUpdateExpiryDateWhenTokenIsValid() {
        // Arrange
        RefreshToken validToken = RefreshToken.builder()
                .token(UUID.randomUUID().toString())
                .expiryDate(Instant.now().plusMillis(1000)) // Not expired
                .build();
        ReflectionTestUtils.setField(validToken, "id", 1L);
        
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(validToken);

        // Act
        RefreshToken result = refreshTokenService.verifyExpiration(validToken);

        // Assert
        assertNotNull(result);
        assertTrue(result.getExpiryDate().isAfter(Instant.now()));
        verify(refreshTokenRepository).save(validToken);
    }

    @Test
    @DisplayName("Should throw exception when token is expired")
    void verifyExpirationShouldThrowExceptionWhenTokenIsExpired() {
        // Arrange
        RefreshToken expiredToken = RefreshToken.builder()
                .token(UUID.randomUUID().toString())
                .expiryDate(Instant.now().minusMillis(1000)) // Expired
                .build();
        ReflectionTestUtils.setField(expiredToken, "id", 1L);
        
        doNothing().when(refreshTokenRepository).delete(expiredToken);

        // Act & Assert
        TokenRefreshException exception = assertThrows(
                TokenRefreshException.class,
                () -> refreshTokenService.verifyExpiration(expiredToken)
        );
        assertTrue(exception.getMessage().contains("Refresh token was expired"));
        verify(refreshTokenRepository).delete(expiredToken);
    }

    @Test
    @DisplayName("Should delete refresh tokens by user ID")
    void deleteByUserIdShouldDeleteTokens() {
        // Arrange
        Long userId = 1L;
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        doNothing().when(refreshTokenRepository).deleteByUser(testUser);

        // Act
        refreshTokenService.deleteByUserId(userId);

        // Assert
        verify(userRepository).findById(userId);
        verify(refreshTokenRepository).deleteByUser(testUser);
    }

    @Test
    @DisplayName("Should throw exception when user is not found during token deletion")
    void deleteByUserIdShouldThrowExceptionWhenUserNotFound() {
        // Arrange
        Long userId = 999L;
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> refreshTokenService.deleteByUserId(userId)
        );
        assertTrue(exception.getMessage().contains("User not found with id: " + userId));
        verify(userRepository).findById(userId);
        verifyNoInteractions(refreshTokenRepository);
    }

    @Test
    @DisplayName("Should return refresh token duration")
    void getRefreshTokenDurationShouldReturnDuration() {
        // Act
        long result = refreshTokenService.getRefreshTokenDuration();

        // Assert
        assertEquals(refreshTokenDurationMs, result);
    }

    private static <T extends BaseEntity> T setId(T entity, Long id) {
        ReflectionTestUtils.setField(entity, "id", id);
        return entity;
    }
} 