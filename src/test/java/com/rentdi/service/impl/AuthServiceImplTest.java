package com.rentdi.service.impl;

import com.rentdi.Repository.RoleRepository;
import com.rentdi.Repository.UserRepository;
import com.rentdi.dto.request.LoginRequest;
import com.rentdi.dto.request.RegisterRequest;
import com.rentdi.dto.request.TokenRefreshRequest;
import com.rentdi.dto.response.AuthResponse;
import com.rentdi.dto.response.TokenRefreshResponse;
import com.rentdi.entity.RefreshToken;
import com.rentdi.entity.Role;
import com.rentdi.entity.User;
import com.rentdi.entity.enums.RoleName;
import com.rentdi.exception.ResourceNotFoundException;
import com.rentdi.exception.TokenRefreshException;
import com.rentdi.mapper.UserMapper;
import com.rentdi.service.RefreshTokenService;
import com.rentdi.util.JwtUtil;
import com.rentdi.util.TokenBlacklist;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.rentdi.entity.BaseEntity;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserMapper userMapper;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private TokenBlacklist tokenBlacklist;

    @Mock
    private HttpServletRequest httpServletRequest;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private AuthServiceImpl authService;

    private User testUser;
    private Role testRole;
    private RefreshToken testRefreshToken;
    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private TokenRefreshRequest tokenRefreshRequest;
    private AuthResponse authResponse;

    @BeforeEach
    void setUp() {
        // Create test role
        testRole = new Role();
        testRole.setId(1L);
        testRole.setName(RoleName.TENANT);

        // Create test user
        Set<Role> roles = new HashSet<>();
        roles.add(testRole);
        
        testUser = User.builder()
                .firstName("Test")
                .lastName("User")
                .email("test@example.com")
                .password("encodedPassword")
                .phoneNumber("+1234567890")
                .profileImage("profile.jpg")
                .isEnabled(true)
                .isLoggedIn(true)
                .roles(roles)
                .build();
        ReflectionTestUtils.setField(testUser, "id", 1L);
        ReflectionTestUtils.setField(testUser, "createdAt", LocalDateTime.now());
        ReflectionTestUtils.setField(testUser, "updatedAt", LocalDateTime.now());

        // Create test refresh token
        testRefreshToken = RefreshToken.builder()
                .token("refresh-token")
                .user(testUser)
                .expiryDate(Instant.now().plusSeconds(86400))
                .build();

        // Create test requests
        registerRequest = RegisterRequest.builder()
                .firstName("Test")
                .lastName("User")
                .email("test@example.com")
                .password("Password1@")
                .phoneNumber("+1234567890")
                .profileImage("profile.jpg")
                .roleName(RoleName.TENANT)
                .build();

        loginRequest = LoginRequest.builder()
                .email("test@example.com")
                .password("Password1@")
                .build();

        tokenRefreshRequest = TokenRefreshRequest.builder()
                .refreshToken("refresh-token")
                .build();

        // Create test response
        authResponse = AuthResponse.builder()
                .id(1L)
                .firstName("Test")
                .lastName("User")
                .email("test@example.com")
                .phoneNumber("+1234567890")
                .profileImage("profile.jpg")
                .isEnabled(true)
                .isLoggedIn(true)
                .token("jwt-token")
                .refreshToken("refresh-token")
                .build();
    }

    @Test
    @DisplayName("Should register a new user successfully")
    void registerShouldCreateNewUserAndReturnAuthResponse() {
        // Arrange
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userMapper.toEntity(any(RegisterRequest.class))).thenReturn(testUser);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(roleRepository.findByName(any(RoleName.class))).thenReturn(Optional.of(testRole));
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(jwtUtil.generateToken(any(User.class))).thenReturn("jwt-token");
        when(refreshTokenService.createRefreshToken(any(User.class))).thenReturn(testRefreshToken);
        when(userMapper.toAuthResponse(any(User.class), anyString(), anyString())).thenReturn(authResponse);

        // Act
        AuthResponse result = authService.register(registerRequest);

        // Assert
        assertNotNull(result);
        assertEquals(authResponse, result);
        verify(userRepository).existsByEmail("test@example.com");
        verify(userMapper).toEntity(registerRequest);
        verify(passwordEncoder).encode("Password1@");
        verify(roleRepository).findByName(RoleName.TENANT);
        verify(userRepository).save(testUser);
        verify(jwtUtil).generateToken(testUser);
        verify(refreshTokenService).createRefreshToken(testUser);
        verify(userMapper).toAuthResponse(testUser, "jwt-token", "refresh-token");
    }

    @Test
    @DisplayName("Should throw exception when email already exists during registration")
    void registerShouldThrowExceptionWhenEmailExists() {
        // Arrange
        when(userRepository.existsByEmail(anyString())).thenReturn(true);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> authService.register(registerRequest)
        );
        assertEquals("Email is already in use", exception.getMessage());
        verify(userRepository).existsByEmail("test@example.com");
        verifyNoInteractions(userMapper, passwordEncoder, roleRepository, jwtUtil, refreshTokenService);
    }

    @Test
    @DisplayName("Should login user successfully")
    void loginShouldAuthenticateUserAndReturnAuthResponse() {
        // Arrange
        UsernamePasswordAuthenticationToken authToken = 
                new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword());
        
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(testUser);
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(jwtUtil.generateToken(any(User.class))).thenReturn("jwt-token");
        when(refreshTokenService.createRefreshToken(any(User.class))).thenReturn(testRefreshToken);
        when(userMapper.toAuthResponse(any(User.class), anyString(), anyString())).thenReturn(authResponse);

        // Act
        AuthResponse result = authService.login(loginRequest);

        // Assert
        assertNotNull(result);
        assertEquals(authResponse, result);
        verify(authenticationManager).authenticate(argThat(auth -> 
                auth.getPrincipal().equals(loginRequest.getEmail()) && 
                auth.getCredentials().equals(loginRequest.getPassword())));
        verify(authentication).getPrincipal();
        verify(userRepository).save(testUser);
        verify(jwtUtil).generateToken(testUser);
        verify(refreshTokenService).createRefreshToken(testUser);
        verify(userMapper).toAuthResponse(testUser, "jwt-token", "refresh-token");
    }

    @Test
    @DisplayName("Should refresh token successfully")
    void refreshTokenShouldReturnNewTokens() {
        // Arrange
        when(refreshTokenService.findByToken(anyString())).thenReturn(Optional.of(testRefreshToken));
        when(refreshTokenService.verifyExpiration(any(RefreshToken.class))).thenReturn(testRefreshToken);
        when(jwtUtil.generateToken(any(User.class))).thenReturn("new-jwt-token");

        // Act
        TokenRefreshResponse result = authService.refreshToken(tokenRefreshRequest);

        // Assert
        assertNotNull(result);
        assertEquals("new-jwt-token", result.getAccessToken());
        assertEquals("refresh-token", result.getRefreshToken());
        assertEquals("Bearer", result.getTokenType());
        verify(refreshTokenService).findByToken("refresh-token");
        verify(refreshTokenService).verifyExpiration(testRefreshToken);
        verify(jwtUtil).generateToken(testUser);
    }

    @Test
    @DisplayName("Should throw exception when refresh token is not found")
    void refreshTokenShouldThrowExceptionWhenTokenNotFound() {
        // Arrange
        when(refreshTokenService.findByToken(anyString())).thenReturn(Optional.empty());

        // Act & Assert
        TokenRefreshException exception = assertThrows(
                TokenRefreshException.class,
                () -> authService.refreshToken(tokenRefreshRequest)
        );
        assertTrue(exception.getMessage().contains("Refresh token is not in database"));
        verify(refreshTokenService).findByToken("refresh-token");
        verifyNoMoreInteractions(refreshTokenService);
        verifyNoInteractions(jwtUtil);
    }

    private static <T extends BaseEntity> T setId(T entity, Long id) {
        ReflectionTestUtils.setField(entity, "id", id);
        return entity;
    }
} 