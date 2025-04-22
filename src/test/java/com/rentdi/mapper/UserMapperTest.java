package com.rentdi.mapper;

import com.rentdi.dto.request.RegisterRequest;
import com.rentdi.dto.response.AuthResponse;
import com.rentdi.entity.Role;
import com.rentdi.entity.User;
import com.rentdi.entity.enums.RoleName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class UserMapperTest {

    private UserMapper userMapper;
    private RegisterRequest registerRequest;
    private User user;
    private Role role;
    private Set<Role> roles;
    private final LocalDateTime now = LocalDateTime.now();

    @BeforeEach
    void setUp() {
        userMapper = new UserMapper();
        
        // Create register request
        registerRequest = RegisterRequest.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .password("Password123!")
                .phoneNumber("+1234567890")
                .profileImage("profile.jpg")
                .roleName(RoleName.TENANT)
                .build();
        
        // Create role
        role = Role.builder()
                .name(RoleName.TENANT)
                .build();
        ReflectionTestUtils.setField(role, "id", 1L);
        
        // Create roles set
        roles = new HashSet<>();
        roles.add(role);
        
        // Create user
        user = User.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .password("encodedPassword")
                .phoneNumber("+1234567890")
                .profileImage("profile.jpg")
                .isEnabled(true)
                .isLoggedIn(true)
                .roles(roles)
                .build();
        ReflectionTestUtils.setField(user, "id", 1L);
        ReflectionTestUtils.setField(user, "createdAt", now);
        ReflectionTestUtils.setField(user, "updatedAt", now);
    }

    @Test
    @DisplayName("Should map RegisterRequest to User entity")
    void toEntityShouldMapRegisterRequestToUserEntity() {
        // Act
        User result = userMapper.toEntity(registerRequest);
        
        // Assert
        assertNotNull(result);
        assertEquals(registerRequest.getFirstName(), result.getFirstName());
        assertEquals(registerRequest.getLastName(), result.getLastName());
        assertEquals(registerRequest.getEmail(), result.getEmail());
        assertEquals(registerRequest.getPassword(), result.getPassword());
        assertEquals(registerRequest.getPhoneNumber(), result.getPhoneNumber());
        assertEquals(registerRequest.getProfileImage(), result.getProfileImage());
        assertTrue(result.getIsEnabled());
        assertFalse(result.getIsLoggedIn());
    }

    @Test
    @DisplayName("Should map User to AuthResponse with refresh token")
    void toAuthResponseShouldMapUserToAuthResponseWithRefreshToken() {
        // Arrange
        String token = "jwt.token.here";
        String refreshToken = "refresh.token.here";
        
        // Act
        AuthResponse result = userMapper.toAuthResponse(user, token, refreshToken);
        
        // Assert
        assertNotNull(result);
        assertEquals(user.getId(), result.getId());
        assertEquals(user.getFirstName(), result.getFirstName());
        assertEquals(user.getLastName(), result.getLastName());
        assertEquals(user.getEmail(), result.getEmail());
        assertEquals(user.getPhoneNumber(), result.getPhoneNumber());
        assertEquals(user.getProfileImage(), result.getProfileImage());
        assertEquals(user.getIsEnabled(), result.getIsEnabled());
        assertEquals(user.getIsLoggedIn(), result.getIsLoggedIn());
        assertEquals(token, result.getToken());
        assertEquals(refreshToken, result.getRefreshToken());
        assertEquals(user.getCreatedAt(), result.getCreatedAt());
        assertEquals(user.getUpdatedAt(), result.getUpdatedAt());
        
        // Verify roles
        assertEquals(1, result.getRoles().size());
        assertTrue(result.getRoles().contains(RoleName.TENANT));
    }
    
    @Test
    @DisplayName("Should map User to AuthResponse without refresh token")
    void toAuthResponseShouldMapUserToAuthResponseWithoutRefreshToken() {
        // Arrange
        String token = "jwt.token.here";
        
        // Act
        AuthResponse result = userMapper.toAuthResponse(user, token, null);
        
        // Assert
        assertNotNull(result);
        assertEquals(user.getId(), result.getId());
        assertEquals(token, result.getToken());
        assertNull(result.getRefreshToken(), "Refresh token should be null");
    }
} 