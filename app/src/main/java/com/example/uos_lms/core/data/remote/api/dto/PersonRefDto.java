package com.example.uos_lms.core.data.remote.api.dto;

import com.google.gson.annotations.SerializedName;

import lombok.Data;
import lombok.NoArgsConstructor;

/** Shape of a populated person-ref field across several controllers (ExamResult's
 * studentId/submittedBy, Assignment/Quiz/Leave/Material's studentId/uploadedBy, Conversation's
 * participantAId/BId) - reused everywhere since an unselected field just stays null (e.g. a
 * teacher ref leaves rollNumber/registrationNumber null, most refs leave role null). */
@Data
@NoArgsConstructor
public class PersonRefDto {
    @SerializedName("_id")
    private String id;
    private String fullName;
    private String rollNumber;
    private String registrationNumber;
    private String role;
}
