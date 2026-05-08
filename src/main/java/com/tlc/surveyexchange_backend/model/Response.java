package com.tlc.surveyexchange_backend.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(
        name = "responses",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_response_respondent_survey",
                columnNames = {"respondent_id", "survey_id"}
        ),
        indexes = @Index(name = "idx_responses_respondent_survey", columnList = "respondent_id, survey_id")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Response {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "respondent_id", nullable = false)
    private User respondent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "survey_id", nullable = false)
    private Survey survey;

    @OneToMany(mappedBy = "response", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Answer> answers = new ArrayList<>();

    @Column
    private Integer timeToCompleteSeconds;

    @Enumerated(EnumType.STRING)
    @Column
    private AcademicLevel respondentAcademicLevel;

    @Column
    private String respondentFieldOfStudy;

    @Column
    private String respondentInstitution;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant respondedAt;
}
