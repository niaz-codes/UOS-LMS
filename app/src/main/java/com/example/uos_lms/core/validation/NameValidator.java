package com.example.uos_lms.core.validation;

import java.util.regex.Pattern;

public final class NameValidator {

    private static final Pattern NAME_REGEX = Pattern.compile("^[A-Za-z ]{2,60}$");

    private NameValidator() {
    }

    public static ValidationResult validate(String name, String fieldLabel) {
        String trimmed = name == null ? "" : name.trim();
        if (trimmed.isEmpty()) {
            return ValidationResult.invalid(fieldLabel + " is required");
        }
        if (!NAME_REGEX.matcher(trimmed).matches()) {
            return ValidationResult.invalid(fieldLabel + " must contain only letters and spaces");
        }
        return ValidationResult.valid();
    }
}
