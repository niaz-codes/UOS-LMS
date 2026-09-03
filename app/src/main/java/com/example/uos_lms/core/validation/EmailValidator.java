package com.example.uos_lms.core.validation;

import java.util.regex.Pattern;

public final class EmailValidator {

    private static final Pattern EMAIL_REGEX =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private EmailValidator() {
    }

    public static ValidationResult validate(String email) {
        String trimmed = email == null ? "" : email.trim();
        if (trimmed.isEmpty()) {
            return ValidationResult.invalid("Email is required");
        }
        if (!EMAIL_REGEX.matcher(trimmed).matches()) {
            return ValidationResult.invalid("Enter a valid email address");
        }
        return ValidationResult.valid();
    }
}
