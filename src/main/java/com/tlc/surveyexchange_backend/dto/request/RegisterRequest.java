package com.tlc.surveyexchange_backend.dto.request;

import com.tlc.surveyexchange_backend.model.AcademicLevel;
import com.tlc.surveyexchange_backend.model.AgeRange;
import com.tlc.surveyexchange_backend.model.Gender;
import com.tlc.surveyexchange_backend.model.ReferralSource;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "Full name is required")
    private String fullName;

    @Email(message = "Must be a valid email address")
    @NotBlank(message = "Email is required")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    @NotBlank(message = "Institution is required")
    private String institution;

    @NotBlank(message = "Field of study is required")
    private String fieldOfStudy;

    @NotNull(message = "Academic level is required")
    private AcademicLevel academicLevel;

    private Integer yearOfStudy;

    private Gender gender;

    private AgeRange ageRange;

    private ReferralSource referralSource;
}
