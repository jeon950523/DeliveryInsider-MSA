package com.deliveryinsider.auth.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(

    @NotBlank
    @Email
    String email,

    @NotBlank
    @Size(
        min = PasswordPolicy.MIN_LENGTH,
        max = PasswordPolicy.MAX_LENGTH,
        message = PasswordPolicy.LENGTH_MESSAGE
    )
    @Pattern(
        regexp = PasswordPolicy.REGEXP,
        message = PasswordPolicy.COMPOSITION_MESSAGE
    )
    String password
) {
}
