package com.nguyen.motoparts.auth.dto;

public record AuthResponse(String token, String tokenType, long expiresInSeconds, UserInfo user) {
}
