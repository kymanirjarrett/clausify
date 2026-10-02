package com.clausify.user;

/** Public view of a user. Never includes the password hash. */
public record UserResponse(Long id, String name, String email) {

    static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail());
    }
}
