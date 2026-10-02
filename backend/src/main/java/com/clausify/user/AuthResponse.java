package com.clausify.user;

/** Returned by register and login: the access token to send as {@code Authorization: Bearer <token>}. */
public record AuthResponse(String token, UserResponse user) {
}
