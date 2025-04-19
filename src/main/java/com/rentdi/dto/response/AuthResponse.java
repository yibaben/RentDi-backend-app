package com.rentdi.dto.response;

import com.rentdi.entity.enums.RoleName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponse {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private String profileImage;
    private Boolean isEnabled;
    private Boolean isLoggedIn;
    private Set<RoleName> roles;
    private String token;
    private String refreshToken;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
} 