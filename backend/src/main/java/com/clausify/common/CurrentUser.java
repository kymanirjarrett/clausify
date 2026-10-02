package com.clausify.common;

import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Reads the signed-in user's id from a validated token. Controllers take
 * {@code @AuthenticationPrincipal Jwt jwt} and pass {@code CurrentUser.id(jwt)} to services,
 * which scope every query to it.
 */
public final class CurrentUser {

    private CurrentUser() {
    }

    /** JwtService puts the user id in the token's subject. */
    public static Long id(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
