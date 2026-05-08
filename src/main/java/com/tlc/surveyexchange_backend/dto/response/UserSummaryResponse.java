package com.tlc.surveyexchange_backend.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.tlc.surveyexchange_backend.model.AcademicLevel;
import com.tlc.surveyexchange_backend.model.AgeRange;
import com.tlc.surveyexchange_backend.model.Gender;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSummaryResponse {
    private UUID id;
    private String name;
    private String email;
    private int creditBalance;

    private String institution;

    private String fieldOfStudy;

    private AcademicLevel academicLevel;

    private Integer yearOfStudy;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Gender gender;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private AgeRange ageRange;

}
