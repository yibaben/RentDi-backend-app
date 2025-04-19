package com.rentdi.controller;

import com.rentdi.dto.request.LoginRequest;
import com.rentdi.dto.request.RegisterRequest;
import com.rentdi.dto.request.TokenRefreshRequest;
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
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return new ResponseEntity<>(authService.register(request), HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
    
    @PostMapping("/refreshtoken")
    public ResponseEntity<TokenRefreshResponse> refreshToken(@Valid @RequestBody TokenRefreshRequest request) {
        return ResponseEntity.ok(authService.refreshToken(request));
    }
    
    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MessageResponse> logout(HttpServletRequest request) {
        String message = authService.logout(request);
        return ResponseEntity.ok(new MessageResponse(message));
    }
} 