package com.example.uos_lms.core.validation;

public final class CnicValidator {

    private CnicValidator() {
    }

    public static String normalize(String raw) {
        if (raw == null) return "";
        StringBuilder digits = new StringBuilder();
        for (char c : raw.toCharArray()) {
            if (Character.isDigit(c)) digits.append(c);
        }
        return digits.toString();
    }

    public static ValidationResult validate(String raw) {
        String digits = normalize(raw);
        if (digits.isEmpty()) {
            return ValidationResult.invalid("CNIC is required");
        }
        if (digits.length() != 13) {
            return ValidationResult.invalid("CNIC must be 13 digits");
        }
        return ValidationResult.valid();
    }
}
