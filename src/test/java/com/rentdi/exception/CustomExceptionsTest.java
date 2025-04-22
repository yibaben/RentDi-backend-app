package com.rentdi.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.*;

class CustomExceptionsTest {

    @Test
    @DisplayName("ResourceNotFoundException should be created with message")
    void resourceNotFoundExceptionShouldBeCreatedWithMessage() {
        // Arrange
        String errorMessage = "Resource not found";

        // Act
        ResourceNotFoundException exception = new ResourceNotFoundException(errorMessage);

        // Assert
        assertEquals(errorMessage, exception.getMessage());
        assertEquals(HttpStatus.NOT_FOUND, exception.getClass().getAnnotation(org.springframework.web.bind.annotation.ResponseStatus.class).value());
    }

    @Test
    @DisplayName("ResourceNotFoundException should be created with message and cause")
    void resourceNotFoundExceptionShouldBeCreatedWithMessageAndCause() {
        // Arrange
        String errorMessage = "Resource not found";
        Throwable cause = new RuntimeException("Original error");

        // Act
        ResourceNotFoundException exception = new ResourceNotFoundException(errorMessage, cause);

        // Assert
        assertEquals(errorMessage, exception.getMessage());
        assertEquals(cause, exception.getCause());
    }

    @Test
    @DisplayName("TokenRefreshException should be created with token and message")
    void tokenRefreshExceptionShouldBeCreatedWithTokenAndMessage() {
        // Arrange
        String token = "invalid.token";
        String message = "Token has expired";
        String expectedMessage = String.format("Failed for [%s]: %s", token, message);

        // Act
        TokenRefreshException exception = new TokenRefreshException(token, message);

        // Assert
        assertEquals(expectedMessage, exception.getMessage());
        assertEquals(HttpStatus.FORBIDDEN, exception.getClass().getAnnotation(org.springframework.web.bind.annotation.ResponseStatus.class).value());
    }

    @Test
    @DisplayName("AccessDeniedException should be created with message")
    void accessDeniedExceptionShouldBeCreatedWithMessage() {
        // Arrange
        String errorMessage = "Access denied";

        // Act
        AccessDeniedException exception = new AccessDeniedException(errorMessage);

        // Assert
        assertEquals(errorMessage, exception.getMessage());
        assertEquals(HttpStatus.FORBIDDEN, exception.getClass().getAnnotation(org.springframework.web.bind.annotation.ResponseStatus.class).value());
    }

    @Test
    @DisplayName("AccessDeniedException should be created with message and cause")
    void accessDeniedExceptionShouldBeCreatedWithMessageAndCause() {
        // Arrange
        String errorMessage = "Access denied";
        Throwable cause = new RuntimeException("Original error");

        // Act
        AccessDeniedException exception = new AccessDeniedException(errorMessage, cause);

        // Assert
        assertEquals(errorMessage, exception.getMessage());
        assertEquals(cause, exception.getCause());
    }
} 