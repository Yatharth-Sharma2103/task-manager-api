package org.example.taskmanager.dto;

public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresInMs,
        String username,
        String role
) {
    public static AuthResponse bearer(String token, long expiresInMs, String username, String role) {
        return new AuthResponse(token, "Bearer", expiresInMs, username, role);
    }
}
