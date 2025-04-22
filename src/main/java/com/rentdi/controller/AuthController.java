package com.rentdi.controller;

import com.rentdi.dto.request.LoginRequest;
import com.rentdi.dto.request.RegisterRequest;
import com.rentdi.dto.request.TokenRefreshRequest;
import com.rentdi.dto.response.ApiResponse;
import com.rentdi.dto.response.AuthResponse;
import com.rentdi.dto.response.MessageResponse;
import com.rentdi.dto.response.TokenRefreshResponse;
import com.rentdi.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse authResponse = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(authResponse));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse authResponse = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful", authResponse));
    }
    
    @PostMapping("/refreshtoken")
    public ResponseEntity<ApiResponse<TokenRefreshResponse>> refreshToken(@Valid @RequestBody TokenRefreshRequest request) {
        TokenRefreshResponse tokenRefreshResponse = authService.refreshToken(request);
        return ResponseEntity.ok(ApiResponse.success("Token refreshed successfully", tokenRefreshResponse));
    }
    
    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<MessageResponse>> logout(HttpServletRequest request) {
        String message = authService.logout(request);
        return ResponseEntity.ok(ApiResponse.success(message, new MessageResponse(message)));
    }
} 