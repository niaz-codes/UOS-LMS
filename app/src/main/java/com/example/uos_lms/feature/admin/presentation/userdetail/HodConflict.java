package com.example.uos_lms.feature.admin.presentation.userdetail;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class HodConflict {
    private final String departmentId;
    private final String departmentName;
    private final String existingHodUid;
    private final String existingHodName;
}
