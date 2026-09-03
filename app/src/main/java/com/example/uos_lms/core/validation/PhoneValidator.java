package com.example.uos_lms.core.validation;

import java.util.regex.Pattern;

public final class PhoneValidator {

    private static final Pattern PATTERN = Pattern.compile("^03[0-9]{9}$");

    private PhoneValidator() {
    }

    public static String normalize(String raw) {
        if (raw == null) return "";
        StringBuilder digits = new StringBuilder();
        for (char c : raw.toCharArray()) {
            if (Character.isDigit(c)) digits.append(c);
        }
        String result = digits.toString();
        if (result.startsWith("92")) {
            result = "0" + result.substring(2);
        }
        return result;
    }

    public static ValidationResult validate(String raw) {
        String normalized = normalize(raw);
        if (normalized.isEmpty()) {
            return ValidationResult.invalid("Phone number is required");
        }
        if (!PATTERN.matcher(normalized).matches()) {
            return ValidationResult.invalid("Enter a valid phone number, e.g. 03001234567");
        }
        return ValidationResult.valid();
    }
}
