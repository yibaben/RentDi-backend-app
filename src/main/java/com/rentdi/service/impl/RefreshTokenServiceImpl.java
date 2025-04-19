package com.rentdi.service.impl;

import com.rentdi.Repository.RefreshTokenRepository;
import com.rentdi.Repository.UserRepository;
import com.rentdi.entity.RefreshToken;
import com.rentdi.entity.User;
import com.rentdi.exception.ResourceNotFoundException;
import com.rentdi.exception.TokenRefreshException;
import com.rentdi.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
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
        // Delete any existing refresh token for this user
        refreshTokenRepository.findByUser(user).ifPresent(refreshTokenRepository::delete);
        
        // Create new refresh token
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(UUID.randomUUID().toString())
                .expiryDate(Instant.now().plusMillis(refreshTokenDurationMs))
                .build();

        return refreshTokenRepository.save(refreshToken);
    }

    @Override
    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.getExpiryDate().compareTo(Instant.now()) < 0) {
            refreshTokenRepository.delete(token);
            throw new TokenRefreshException(token.getToken(), "Refresh token was expired. Please make a new sign in request");
        }
        
        // Update expiry time
        token.setExpiryDate(Instant.now().plusMillis(refreshTokenDurationMs));
        return refreshTokenRepository.save(token);
    }

    @Override
    @Transactional
    public void deleteByUserId(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        
        refreshTokenRepository.deleteByUser(user);
    }
} 