package com.example.uos_lms.feature.admin.domain.model;

public enum UserSortOption {
    NAME_ASC("Name (A-Z)"),
    NAME_DESC("Name (Z-A)"),
    STATUS("Status"),
    NEWEST("Newest");

    private final String label;

    UserSortOption(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
