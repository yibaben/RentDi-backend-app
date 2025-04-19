package com.rentdi.config;

import com.rentdi.util.TokenBlacklist;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@EnableScheduling
@RequiredArgsConstructor
public class SchedulingConfig {

    private final TokenBlacklist tokenBlacklist;
    
    // Run every hour to clean up expired blacklisted tokens
    @Scheduled(fixedRate = 3600000)
    public void cleanupExpiredTokens() {
        tokenBlacklist.cleanupExpiredTokens();
    }
} 