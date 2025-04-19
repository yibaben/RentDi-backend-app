package com.rentdi.service;

import com.rentdi.entity.RefreshToken;
import com.rentdi.entity.User;

import java.util.Optional;

public interface RefreshTokenService {
    RefreshToken createRefreshToken(User user);
    Optional<RefreshToken> findByToken(String token);
    RefreshToken verifyExpiration(RefreshToken token);
    void deleteByUserId(Long userId);
} 