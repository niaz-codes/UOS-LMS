package com.example.uos_lms.core.data.remote.api.dto;

import java.util.List;

import lombok.Data;
import lombok.NoArgsConstructor;

/** Used by both the generic user list ({user"s"}) and the subject roster endpoint
 * ({"students"}) - Retrofit only reads the field actually present in the response, so one
 * DTO covers both as long as callers use the matching getter. */
@Data
@NoArgsConstructor
public class UsersEnvelopeDto {
    private List<UserResponseDto> users;
    private List<UserResponseDto> students;
}
