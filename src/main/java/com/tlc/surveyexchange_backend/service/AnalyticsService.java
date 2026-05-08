package com.tlc.surveyexchange_backend.service;

import com.tlc.surveyexchange_backend.dto.response.*;
import com.tlc.surveyexchange_backend.exception.ForbiddenOperationException;
import com.tlc.surveyexchange_backend.exception.ResourceNotFoundException;
import com.tlc.surveyexchange_backend.model.*;
import com.tlc.surveyexchange_backend.repository.AnswerRepository;
import com.tlc.surveyexchange_backend.repository.ResponseRepository;
import com.tlc.surveyexchange_backend.repository.SurveyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private static final Set<QuestionType> CHOICE_TYPES = Set.of(
            QuestionType.MULTIPLE_CHOICE,
            QuestionType.CHECKBOX,
            QuestionType.RATING,
            QuestionType.YES_NO
    );

    private final SurveyRepository surveyRepository;
    private final ResponseRepository responseRepository;
    private final AnswerRepository answerRepository;

    @Transactional(readOnly = true)
    public SurveyAnalyticsResponse getAnalytics(UUID surveyId, UUID requesterId) {
        Survey survey = surveyRepository.findByIdWithQuestions(surveyId)
                .orElseThrow(() -> new ResourceNotFoundException("Survey", surveyId));

        if (!survey.getOwner().getId().equals(requesterId)) {
            throw new ForbiddenOperationException("Only the survey owner can access analytics");
        }

        long totalResponses = responseRepository.countBySurveyId(surveyId);

        Double completionPercentage = null;
        if (survey.getTargetResponses() != null && survey.getTargetResponses() > 0) {
            completionPercentage = Math.min(100.0,
                    Math.round((double) totalResponses / survey.getTargetResponses() * 1000.0) / 10.0);
        }

        List<QuestionAnalyticsDTO> questions = survey.getQuestions().stream()
                .map(q -> buildQuestionAnalytics(q, totalResponses))
                .toList();

        return SurveyAnalyticsResponse.builder()
                .surveyId(survey.getId())
                .title(survey.getTitle())
                .totalResponses(totalResponses)
                .targetResponses(survey.getTargetResponses())
                .completionPercentage(completionPercentage)
                .questions(questions)
                .build();
    }

    @Transactional(readOnly = true)
    public PageResponse<ResponseRowDTO> getRawResponses(UUID surveyId, UUID requesterId, int page, int size) {
        Survey survey = surveyRepository.findById(surveyId)
                .orElseThrow(() -> new ResourceNotFoundException("Survey", surveyId));

        if (!survey.getOwner().getId().equals(requesterId)) {
            throw new ForbiddenOperationException("Only the survey owner can access analytics");
        }

        Page<Response> responsePage = responseRepository
                .findBySurveyIdOrderByRespondedAtDesc(surveyId, PageRequest.of(page, size));

        List<UUID> ids = responsePage.getContent().stream().map(Response::getId).toList();

        if (ids.isEmpty()) {
            return PageResponse.from(new PageImpl<>(List.of(), responsePage.getPageable(), 0));
        }

        Map<UUID, Response> responseMap = responseRepository.findWithAnswersByIds(ids).stream()
                .collect(Collectors.toMap(Response::getId, r -> r));

        List<ResponseRowDTO> rows = ids.stream()
                .map(id -> mapToResponseRow(responseMap.getOrDefault(id, null)))
                .filter(Objects::nonNull)
                .toList();

        Page<ResponseRowDTO> dtoPage = new PageImpl<>(rows, responsePage.getPageable(), responsePage.getTotalElements());
        return PageResponse.from(dtoPage);
    }

    private QuestionAnalyticsDTO buildQuestionAnalytics(Question question, long totalResponses) {
        List<String> rawValues = answerRepository.findAnswerValuesByQuestion(question.getId());

        if (CHOICE_TYPES.contains(question.getQuestionType())) {
            Map<String, Long> counts = rawValues.stream()
                    .flatMap(v -> Arrays.stream(v.split(",")))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.groupingBy(s -> s, Collectors.counting()));

            List<OptionDistributionDTO> distribution = counts.entrySet().stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                    .map(e -> OptionDistributionDTO.builder()
                            .option(e.getKey())
                            .count(e.getValue())
                            .percentage(totalResponses > 0
                                    ? Math.round(e.getValue() * 1000.0 / totalResponses) / 10.0
                                    : 0.0)
                            .build())
                    .toList();

            return QuestionAnalyticsDTO.builder()
                    .questionId(question.getId())
                    .text(question.getText())
                    .type(question.getQuestionType())
                    .distribution(distribution)
                    .build();
        }

        return QuestionAnalyticsDTO.builder()
                .questionId(question.getId())
                .text(question.getText())
                .type(question.getQuestionType())
                .textResponses(rawValues)
                .build();
    }

    private ResponseRowDTO mapToResponseRow(Response response) {
        if (response == null) return null;
        List<AnswerRowDTO> answers = response.getAnswers().stream()
                .map(a -> AnswerRowDTO.builder()
                        .questionId(a.getQuestion().getId())
                        .questionText(a.getQuestion().getText())
                        .questionType(a.getQuestion().getQuestionType())
                        .answerValue(a.getAnswerValue())
                        .build())
                .toList();
        return ResponseRowDTO.builder()
                .responseId(response.getId())
                .respondedAt(response.getRespondedAt())
                .answers(answers)
                .build();
    }
}
