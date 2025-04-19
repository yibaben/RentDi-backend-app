package com.rentdi.util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.rentdi.entity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.stream.Collectors;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    public String generateToken(User user) {
        String roles = user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        return JWT.create()
                .withSubject(user.getUsername())
                .withClaim("roles", roles)
                .withClaim("userId", user.getId())
                .withClaim("email", user.getEmail())
                .withClaim("firstName", user.getFirstName())
                .withClaim("lastName", user.getLastName())
                .withIssuedAt(new Date())
                .withExpiresAt(new Date(System.currentTimeMillis() + expiration))
                .sign(Algorithm.HMAC512(secret));
    }

    public String getUsernameFromToken(String token) {
        DecodedJWT jwt = verifyToken(token);
        return jwt.getSubject();
    }

    public Long getUserIdFromToken(String token) {
        DecodedJWT jwt = verifyToken(token);
        return jwt.getClaim("userId").asLong();
    }

    public DecodedJWT verifyToken(String token) {
        try {
            Algorithm algorithm = Algorithm.HMAC512(secret);
            JWTVerifier verifier = JWT.require(algorithm).build();
            return verifier.verify(token);
        } catch (JWTVerificationException e) {
            throw new JWTVerificationException("Invalid JWT token: " + e.getMessage());
        }
    }

    public boolean validateToken(String token) {
        try {
            verifyToken(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
} 