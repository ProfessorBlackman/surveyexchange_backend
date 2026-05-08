package com.tlc.surveyexchange_backend.repository;

import com.tlc.surveyexchange_backend.model.Answer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface AnswerRepository extends JpaRepository<Answer, UUID> {

    @Query("""
            SELECT a.answerValue FROM Answer a
            WHERE a.question.id = :questionId
            AND a.answerValue IS NOT NULL
            AND a.answerValue <> ''
            """)
    List<String> findAnswerValuesByQuestion(@Param("questionId") UUID questionId);
}
