package com.rentdi.service;

import com.rentdi.dto.request.LoginRequest;
import com.rentdi.dto.request.RegisterRequest;
import com.rentdi.dto.request.TokenRefreshRequest;
import com.rentdi.dto.response.AuthResponse;
import com.rentdi.dto.response.TokenRefreshResponse;
import jakarta.servlet.http.HttpServletRequest;

public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    TokenRefreshResponse refreshToken(TokenRefreshRequest request);
    String logout(HttpServletRequest request);
} 