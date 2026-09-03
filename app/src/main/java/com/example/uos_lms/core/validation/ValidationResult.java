package com.example.uos_lms.core.validation;

import androidx.annotation.Nullable;

public final class ValidationResult {

    private final boolean valid;
    private final String message;

    private ValidationResult(boolean valid, String message) {
        this.valid = valid;
        this.message = message;
    }

    public static ValidationResult valid() {
        return new ValidationResult(true, null);
    }

    public static ValidationResult invalid(String message) {
        return new ValidationResult(false, message);
    }

    public boolean isValid() {
        return valid;
    }

    @Nullable
    public String errorMessageOrNull() {
        return valid ? null : message;
    }
}
