package com.example.uos_lms.core.domain.model;

public enum MaterialType {
    // PPT/NOTE are legacy - kept only so any pre-existing material still deserializes and
    // displays correctly. New uploads always pick from the other five (see
    // TeacherSubjectMaterialsFragment's type picker, which only offers those).
    PDF, VIDEO, DOCUMENT, IMAGE, OTHER, PPT, NOTE;

    public static MaterialType fromStringOrNull(String raw) {
        if (raw == null) return null;
        for (MaterialType type : values()) {
            if (type.name().equals(raw)) return type;
        }
        return null;
    }
}
