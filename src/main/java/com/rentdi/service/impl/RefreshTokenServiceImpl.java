package com.rentdi.service.impl;

import com.rentdi.Repository.RefreshTokenRepository;
import com.rentdi.Repository.UserRepository;
import com.rentdi.entity.RefreshToken;
import com.rentdi.entity.User;
import com.rentdi.exception.ResourceNotFoundException;
import com.rentdi.exception.TokenRefreshException;
import com.rentdi.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenServiceImpl implements RefreshTokenService {

    @Value("${jwt.refresh.expiration}")
    private Long refreshTokenDurationMs; // 30 days in milliseconds

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    @Override
    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    @Override
    @Transactional
    public RefreshToken createRefreshToken(User user) {
        // Always check for existing token first
        Optional<RefreshToken> existingTokenOpt = refreshTokenRepository.findByUser(user);
        
        if (existingTokenOpt.isPresent()) {
            // If token exists, update it instead of creating a new one
            log.debug("Updating existing refresh token for user ID: {}", user.getId());
            RefreshToken existingToken = existingTokenOpt.get();
            existingToken.setToken(UUID.randomUUID().toString());
            existingToken.setExpiryDate(Instant.now().plusMillis(refreshTokenDurationMs));
            return refreshTokenRepository.save(existingToken);
        } else {
            // If no token exists, create a new one
            try {
                log.debug("Creating new refresh token for user ID: {}", user.getId());
                RefreshToken refreshToken = RefreshToken.builder()
                        .user(user)
                        .token(UUID.randomUUID().toString())
                        .expiryDate(Instant.now().plusMillis(refreshTokenDurationMs))
                        .build();
                
                return refreshTokenRepository.save(refreshToken);
            } catch (DataIntegrityViolationException e) {
                // This is a rare race condition - if token was created between our check and save
                log.warn("Race condition detected when creating refresh token for user ID: {}", user.getId());
                // Handle it with a new transaction to ensure fresh data
                return handleRaceCondition(user);
            }
        }
    }
    
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public RefreshToken handleRaceCondition(User user) {
        // In a new transaction, try again to find and update
        log.debug("Handling race condition for user ID: {}", user.getId());
        Optional<RefreshToken> existingToken = refreshTokenRepository.findByUser(user);
        if (existingToken.isPresent()) {
            log.debug("Found existing token in race condition handler for user ID: {}", user.getId());
            RefreshToken token = existingToken.get();
            token.setToken(UUID.randomUUID().toString());
            token.setExpiryDate(Instant.now().plusMillis(refreshTokenDurationMs));
            return refreshTokenRepository.save(token);
        } else {
            // If we still can't find it but got an integrity violation earlier,
            // try one last approach - delete any tokens for this user
            log.warn("No token found in race condition handler, performing cleanup for user ID: {}", user.getId());
            refreshTokenRepository.deleteByUser(user);
            
            // And create a new one
            RefreshToken refreshToken = RefreshToken.builder()
                    .user(user)
                    .token(UUID.randomUUID().toString())
                    .expiryDate(Instant.now().plusMillis(refreshTokenDurationMs))
                    .build();
            
            return refreshTokenRepository.save(refreshToken);
        }
    }

    @Override
    @Transactional
    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.getExpiryDate().compareTo(Instant.now()) < 0) {
            log.info("Refresh token expired: {}", token.getToken());
            refreshTokenRepository.delete(token);
            throw new TokenRefreshException(token.getToken(), "Refresh token was expired. Please make a new sign in request");
        }
        
        // Update expiry time
        log.debug("Extending expiry time for refresh token: {}", token.getToken());
        token.setExpiryDate(Instant.now().plusMillis(refreshTokenDurationMs));
        return refreshTokenRepository.save(token);
    }

    @Override
    @Transactional
    public void deleteByUserId(Long userId) {
        log.debug("Deleting refresh tokens for user ID: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        
        refreshTokenRepository.deleteByUser(user);
    }

    @Override
    public long getRefreshTokenDuration() {
        return refreshTokenDurationMs;
    }
} 