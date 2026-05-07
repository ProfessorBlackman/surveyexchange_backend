package com.tlc.surveyexchange_backend.repository;

import com.tlc.surveyexchange_backend.model.Answer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface AnswerRepository extends JpaRepository<Answer, UUID> {

    @Query("""
            SELECT aso, COUNT(aso)
            FROM Answer a JOIN a.selectedOptions aso
            WHERE a.question.id = :questionId
            GROUP BY aso
            ORDER BY COUNT(aso) DESC
            """)
    List<Object[]> countOptionsByQuestion(@Param("questionId") UUID questionId);

    @Query("""
            SELECT a.answerText FROM Answer a
            WHERE a.question.id = :questionId
            AND a.answerText IS NOT NULL
            AND a.answerText <> ''
            """)
    List<String> findTextAnswersByQuestion(@Param("questionId") UUID questionId);
}
