package com.example.uos_lms.core.data.remote.api.dto;

import lombok.Builder;
import lombok.Data;

/** clearSession/clearSemester are explicit boolean flags rather than relying on a literal
 * JSON null (Gson omits null fields on serialize by default, which would otherwise silently
 * no-op instead of clearing the assignment). */
@Data
@Builder
public class UpdateAcademicPlacementRequestDto {
    private final String sessionId;
    private final String currentSemesterId;
    private final boolean clearSession;
    private final boolean clearSemester;
}
