package com.example.uos_lms.core.validation;

public final class PasswordValidator {

    private PasswordValidator() {
    }

    public static ValidationResult validate(String password) {
        if (password == null || password.isEmpty()) {
            return ValidationResult.invalid("Password is required");
        }
        if (password.length() < 8) {
            return ValidationResult.invalid("Password must be at least 8 characters");
        }
        boolean hasDigit = false;
        boolean hasLetter = false;
        for (int i = 0; i < password.length(); i++) {
            char c = password.charAt(i);
            if (Character.isDigit(c)) hasDigit = true;
            if (Character.isLetter(c)) hasLetter = true;
        }
        if (!hasDigit) {
            return ValidationResult.invalid("Password must contain at least one digit");
        }
        if (!hasLetter) {
            return ValidationResult.invalid("Password must contain at least one letter");
        }
        return ValidationResult.valid();
    }

    public static ValidationResult validateConfirmation(String password, String confirmation) {
        if (confirmation == null || confirmation.isEmpty()) {
            return ValidationResult.invalid("Please confirm your password");
        }
        if (!confirmation.equals(password)) {
            return ValidationResult.invalid("Passwords do not match");
        }
        return ValidationResult.valid();
    }
}
