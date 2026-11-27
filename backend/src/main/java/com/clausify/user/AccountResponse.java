package com.clausify.user;

/** FR-12: the signed-in user's details for the Account page. Never includes the password hash. */
public record AccountResponse(Long id, String name, String email, String phoneNumber) {

    static AccountResponse from(User user) {
        Profile profile = user.getProfile();
        return new AccountResponse(user.getId(), user.getName(), user.getEmail(),
                profile == null ? null : profile.getPhoneNumber());
    }
}
