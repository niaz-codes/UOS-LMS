package com.example.uos_lms.core.data.remote.api.dto;

import com.google.gson.annotations.SerializedName;

import lombok.Data;
import lombok.NoArgsConstructor;

/** Shape of a populated `subjectId` field on ExamResult/RepeatExam responses (see
 * backend/src/controllers/examResultController.js POPULATE_SUBJECT). */
@Data
@NoArgsConstructor
public class SubjectRefDto {
    @SerializedName("_id")
    private String id;
    private String code;
    private String title;
    private Integer creditHours;
    private String departmentId;
    private String semesterId;
}
