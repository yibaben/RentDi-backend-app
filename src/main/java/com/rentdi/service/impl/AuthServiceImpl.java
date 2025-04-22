package com.rentdi.service.impl;

import com.rentdi.Repository.RoleRepository;
import com.rentdi.Repository.UserRepository;
import com.rentdi.dto.request.LoginRequest;
import com.rentdi.dto.request.LogoutRequest;
import com.rentdi.dto.request.RegisterRequest;
import com.rentdi.dto.request.TokenRefreshRequest;
import com.rentdi.dto.response.AuthResponse;
import com.rentdi.dto.response.TokenRefreshResponse;
import com.rentdi.entity.RefreshToken;
import com.rentdi.entity.Role;
import com.rentdi.entity.User;
import com.rentdi.entity.enums.RoleName;
import com.rentdi.exception.AccessDeniedException;
import com.rentdi.exception.ResourceNotFoundException;
import com.rentdi.exception.TokenRefreshException;
import com.rentdi.mapper.UserMapper;
import com.rentdi.service.AuthService;
import com.rentdi.service.RefreshTokenService;
import com.rentdi.util.JwtUtil;
import com.rentdi.util.TokenBlacklist;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;
    private final TokenBlacklist tokenBlacklist;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // Check if user already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email is already in use");
        }

        // Map request to user entity
        User user = userMapper.toEntity(request);
        
        // Encode password
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        
        // Assign role
        Set<Role> roles = new HashSet<>();
        RoleName roleName = request.getRoleName();
        
        if (roleName == null) {
            roleName = RoleName.TENANT; // Default role
        }
        
        final RoleName finalRoleName = roleName; // Create a final variable for use in lambda
        Role role = roleRepository.findByName(finalRoleName)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found with name: " + finalRoleName));
        
        roles.add(role);
        user.setRoles(roles);
        
        // Set user as logged in since we're providing an auth token
        user.setIsLoggedIn(true);
        
        // Save user
        User savedUser = userRepository.save(user);
        
        // Generate JWT token
        String token = jwtUtil.generateToken(savedUser);
        
        // Generate refresh token
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(savedUser);
        
        // Return response
        return userMapper.toAuthResponse(savedUser, token, refreshToken.getToken());
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        try {
            // Authenticate user
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
            
            SecurityContextHolder.getContext().setAuthentication(authentication);
            
            // Get user from authentication
            User user = (User) authentication.getPrincipal();
            
            // Update login status
            user.setIsLoggedIn(true);
            userRepository.save(user);
            
            // Generate JWT token
            String token = jwtUtil.generateToken(user);
            
            // Get or create refresh token, handling any errors
            RefreshToken refreshToken;
            String refreshTokenValue;
            try {
                // Try to create or update refresh token
                refreshToken = refreshTokenService.createRefreshToken(user);
                refreshTokenValue = refreshToken.getToken();
            } catch (Exception e) {
                // If there's any issue with refresh token, log it but don't fail the login
                log.error("Error creating refresh token: {}", e.getMessage(), e);
                
                // As a fallback, try to delete all refresh tokens for this user and create a new one
                try {
                    refreshTokenService.deleteByUserId(user.getId());
                    refreshToken = RefreshToken.builder()
                            .user(user)
                            .token(UUID.randomUUID().toString())
                            .expiryDate(Instant.now().plusMillis(refreshTokenService.getRefreshTokenDuration()))
                            .build();
                    refreshTokenValue = refreshToken.getToken();
                } catch (Exception ex) {
                    // If even that fails, just return a login response without a refresh token
                    log.error("Fallback refresh token creation failed: {}", ex.getMessage(), ex);
                    refreshTokenValue = null;
                }
            }
            
            // Return response
            return userMapper.toAuthResponse(user, token, refreshTokenValue);
        } catch (Exception e) {
            // Log the specific error for debugging
            log.error("Login error: {}", e.getMessage(), e);
            throw e; // Rethrow to be handled by the global exception handler
        }
    }
    
    @Override
    public TokenRefreshResponse refreshToken(TokenRefreshRequest request) {
        String requestRefreshToken = request.getRefreshToken();

        return refreshTokenService.findByToken(requestRefreshToken)
                .map(refreshTokenService::verifyExpiration)
                .map(RefreshToken::getUser)
                .map(user -> {
                    String token = jwtUtil.generateToken(user);
                    return TokenRefreshResponse.builder()
                            .accessToken(token)
                            .refreshToken(requestRefreshToken)
                            .tokenType("Bearer")
                            .build();
                })
                .orElseThrow(() -> new TokenRefreshException(requestRefreshToken,
                        "Refresh token is not in database!"));
    }

    @Override
    @Transactional
    public String logout(HttpServletRequest request) {
        // Extract JWT token from the request
        String currentToken = extractTokenFromRequest(request);
        
        if (currentToken == null || currentToken.isEmpty()) {
            throw new AccessDeniedException("No authentication token provided");
        }
        
        // Get userId from the JWT token
        Long userIdFromToken = jwtUtil.getUserIdFromToken(currentToken);
        
        // Get the authenticated user
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("User is not authenticated");
        }
        
        User user = (User) authentication.getPrincipal();
        
        // Security check: ensure the user in the token matches the authenticated user
        if (!user.getId().equals(userIdFromToken)) {
            throw new AccessDeniedException("Token does not match the authenticated user");
        }
        
        // Add current Auth token to blacklist
        tokenBlacklist.addToBlacklist(currentToken, jwtUtil.verifyToken(currentToken));
        
        // Delete the refresh token
        refreshTokenService.deleteByUserId(user.getId());
        
        // Update user login status
        user.setIsLoggedIn(false);
        userRepository.save(user);
        
        // Clear security context
        SecurityContextHolder.clearContext();
        
        return "Logout successful. You have been logged out.";
    }
    
    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
} 