package com.rentdi.service.impl;

import com.rentdi.Repository.UserRepository;
import com.rentdi.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserDetailsServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserDetailsServiceImpl userDetailsService;

    @Test
    @DisplayName("Should load user by username successfully")
    void loadUserByUsernameShouldReturnUserDetailsWhenUserExists() {
        // Arrange
        String email = "test@example.com";
        User mockUser = mock(User.class);
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(mockUser));

        // Act
        UserDetails result = userDetailsService.loadUserByUsername(email);

        // Assert
        assertNotNull(result);
        assertEquals(mockUser, result);
        verify(userRepository).findByEmail(email);
    }

    @Test
    @DisplayName("Should throw UsernameNotFoundException when user does not exist")
    void loadUserByUsernameShouldThrowExceptionWhenUserDoesNotExist() {
        // Arrange
        String email = "nonexistent@example.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        // Act & Assert
        UsernameNotFoundException exception = assertThrows(
                UsernameNotFoundException.class,
                () -> userDetailsService.loadUserByUsername(email)
        );
        
        assertEquals("User not found with email: " + email, exception.getMessage());
        verify(userRepository).findByEmail(email);
    }
} 