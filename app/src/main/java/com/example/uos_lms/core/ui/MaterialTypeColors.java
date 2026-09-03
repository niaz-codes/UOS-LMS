package com.example.uos_lms.core.ui;

import androidx.annotation.ColorRes;

import com.example.uos_lms.R;
import com.example.uos_lms.core.domain.model.MaterialType;

/** Accent color per study-material type, for item_study_material.xml's accent bar. */
public final class MaterialTypeColors {

    private MaterialTypeColors() {
    }

    @ColorRes
    public static int colorFor(MaterialType type) {
        if (type == null) return R.color.on_surface_variant_color;
        switch (type) {
            case PDF:
                return R.color.status_warning;
            case VIDEO:
                return R.color.status_info;
            case DOCUMENT:
            case PPT:
            case NOTE:
                return R.color.role_teacher_start;
            case IMAGE:
                return R.color.status_success;
            case OTHER:
            default:
                return R.color.on_surface_variant_color;
        }
    }
}
