package com.example.uos_lms.core.data.remote.api.dto;

import com.example.uos_lms.core.data.remote.api.IsoDates;
import com.example.uos_lms.core.domain.model.CalendarEvent;
import com.example.uos_lms.core.domain.model.CalendarEventType;
import com.google.gson.annotations.SerializedName;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class CalendarEventResponseDto {
    @SerializedName("_id")
    private String id;
    private String title;
    private String description;
    private String type;
    private String date;
    private String createdBy;
    private String createdAt;

    public CalendarEvent toDomain() {
        return CalendarEvent.builder()
                .id(id)
                .title(title)
                .description(description)
                .type(CalendarEventType.fromStringOrNull(type))
                .dateMillis(IsoDates.toMillis(date))
                .createdBy(createdBy)
                .createdAt(IsoDates.toMillis(createdAt))
                .build();
    }
}
