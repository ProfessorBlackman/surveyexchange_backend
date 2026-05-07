package com.tlc.surveyexchange_backend.repository;

import com.tlc.surveyexchange_backend.model.Question;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface QuestionRepository extends JpaRepository<Question, UUID> {
}
