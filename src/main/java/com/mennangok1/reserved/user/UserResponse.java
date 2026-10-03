package com.mennangok1.reserved.user;

public record UserResponse(
        Long id,
        String name,
        String email,
        String phoneNumber,
        String role
) {

static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getPhoneNumber(),
                user.getRole().getName()
        );
    }

}
