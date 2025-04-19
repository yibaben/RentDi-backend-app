package com.rentdi.util;

import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TokenBlacklist {
    
    // Store invalidated tokens with their expiry times
    private final Map<String, Date> blacklistedTokens = new ConcurrentHashMap<>();
    
    // Add a token to the blacklist
    public void addToBlacklist(String token, DecodedJWT decodedJWT) {
        blacklistedTokens.put(token, decodedJWT.getExpiresAt());
    }
    
    // Check if a token is blacklisted
    public boolean isBlacklisted(String token) {
        return blacklistedTokens.containsKey(token);
    }
    
    // Clean up expired tokens from the blacklist
    public void cleanupExpiredTokens() {
        Instant now = Instant.now();
        blacklistedTokens.entrySet().removeIf(entry -> entry.getValue().toInstant().isBefore(now));
    }
} 