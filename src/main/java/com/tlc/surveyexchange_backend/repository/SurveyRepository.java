package com.tlc.surveyexchange_backend.repository;

import com.tlc.surveyexchange_backend.model.Survey;
import com.tlc.surveyexchange_backend.model.SurveyStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SurveyRepository extends JpaRepository<Survey, UUID> {

    @Query(value = """
            SELECT s FROM Survey s
            JOIN FETCH s.owner
            WHERE s.owner.id <> :userId
            AND s.status = 'ACTIVE'
            AND s.id NOT IN (
                SELECT r.survey.id FROM Response r WHERE r.respondent.id = :userId
            )
            ORDER BY
                CASE WHEN s.boostExpiresAt > CURRENT_TIMESTAMP THEN 0 ELSE 1 END ASC,
                s.createdAt DESC
            """,
            countQuery = """
            SELECT COUNT(s) FROM Survey s
            WHERE s.owner.id <> :userId
            AND s.status = 'ACTIVE'
            AND s.id NOT IN (
                SELECT r.survey.id FROM Response r WHERE r.respondent.id = :userId
            )
            """)
    Page<Survey> findFeedForUser(@Param("userId") UUID userId, Pageable pageable);

    @Query("SELECT s FROM Survey s JOIN FETCH s.questions WHERE s.id = :id")
    Optional<Survey> findByIdWithQuestions(@Param("id") UUID id);

    List<Survey> findByOwnerIdOrderByCreatedAtDesc(UUID ownerId);

    long countByOwnerId(UUID ownerId);

    @Query("SELECT COUNT(s) FROM Survey s WHERE s.boostExpiresAt > CURRENT_TIMESTAMP")
    long countActiveBoosts();

    @Query(value = """
            SELECT s FROM Survey s
            WHERE (:status IS NULL OR s.status = :status)
            AND (:search IS NULL OR :search = ''
                 OR LOWER(s.title) LIKE LOWER(CONCAT('%', :search, '%')))
            ORDER BY s.createdAt DESC
            """,
            countQuery = """
            SELECT COUNT(s) FROM Survey s
            WHERE (:status IS NULL OR s.status = :status)
            AND (:search IS NULL OR :search = ''
                 OR LOWER(s.title) LIKE LOWER(CONCAT('%', :search, '%')))
            """)
    Page<Survey> findForAdmin(@Param("status") SurveyStatus status,
                              @Param("search") String search,
                              Pageable pageable);
}
