package com.tlc.surveyexchange_backend.repository;

import com.tlc.surveyexchange_backend.model.Response;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ResponseRepository extends JpaRepository<Response, UUID> {

    boolean existsByRespondentIdAndSurveyId(UUID respondentId, UUID surveyId);

    Optional<Response> findByRespondentIdAndSurveyId(UUID respondentId, UUID surveyId);

    long countBySurveyId(UUID surveyId);

    Page<Response> findBySurveyIdOrderByRespondedAtDesc(UUID surveyId, Pageable pageable);

    @Query("""
            SELECT DISTINCT r FROM Response r
            LEFT JOIN FETCH r.answers a
            LEFT JOIN FETCH a.question
            WHERE r.id IN :ids
            ORDER BY r.respondedAt DESC
            """)
    List<Response> findWithAnswersByIds(@Param("ids") List<UUID> ids);

    long countByRespondentId(UUID respondentId);

    List<Response> findByRespondedAtAfter(java.time.Instant since);

    List<Response> findAllBySurveyId(UUID surveyId);
}
