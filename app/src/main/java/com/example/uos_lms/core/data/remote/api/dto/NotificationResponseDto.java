package com.example.uos_lms.core.data.remote.api.dto;

import com.example.uos_lms.core.data.remote.api.IsoDates;
import com.example.uos_lms.core.domain.model.AppNotification;
import com.example.uos_lms.core.domain.model.NotificationCategory;
import com.google.gson.annotations.SerializedName;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class NotificationResponseDto {
    @SerializedName("_id")
    private String id;
    private String type;
    private String category;
    private String title;
    private String body;
    private String relatedType;
    private String relatedId;
    private boolean read;
    private String createdAt;

    public AppNotification toDomain() {
        NotificationCategory parsedCategory;
        try {
            parsedCategory = NotificationCategory.valueOf(category);
        } catch (Exception e) {
            parsedCategory = NotificationCategory.SYSTEM;
        }
        return AppNotification.builder()
                .id(id)
                .type(type)
                .category(parsedCategory)
                .title(title)
                .body(body)
                .relatedType(relatedType)
                .relatedId(relatedId)
                .read(read)
                .createdAtMillis(IsoDates.toMillis(createdAt))
                .build();
    }
}
