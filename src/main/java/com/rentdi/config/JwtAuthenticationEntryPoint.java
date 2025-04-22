package com.rentdi.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        
        final Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("status", HttpStatus.FORBIDDEN.value());
        errorResponse.put("message", "Authentication failed: " + authException.getMessage());
        errorResponse.put("timestamp", LocalDateTime.now().toString());
        
        objectMapper.writeValue(response.getOutputStream(), errorResponse);
    }
} 