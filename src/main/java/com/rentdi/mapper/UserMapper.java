package com.rentdi.mapper;

import com.rentdi.dto.request.RegisterRequest;
import com.rentdi.dto.response.AuthResponse;
import com.rentdi.entity.Role;
import com.rentdi.entity.User;
import com.rentdi.entity.enums.RoleName;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

@Component
public class UserMapper {

    public User toEntity(RegisterRequest request) {
        return User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .password(request.getPassword()) // Password will be encoded in the service
                .phoneNumber(request.getPhoneNumber())
                .profileImage(request.getProfileImage())
                .isEnabled(true)
                .isLoggedIn(false)
                .build();
    }

    public AuthResponse toAuthResponse(User user, String token, String refreshToken) {
        Set<RoleName> roleNames = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());

        AuthResponse.AuthResponseBuilder builder = AuthResponse.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .profileImage(user.getProfileImage())
                .isEnabled(user.getIsEnabled())
                .isLoggedIn(user.getIsLoggedIn())
                .roles(roleNames)
                .token(token)
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt());
                
        // Only add refresh token if it's not null
        if (refreshToken != null) {
            builder.refreshToken(refreshToken);
        }
        
        return builder.build();
    }
} 