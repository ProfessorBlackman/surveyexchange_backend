package com.tlc.surveyexchange_backend.service;

import com.tlc.surveyexchange_backend.dto.request.CreateSurveyRequest;
import com.tlc.surveyexchange_backend.dto.request.QuestionRequest;
import com.tlc.surveyexchange_backend.dto.request.UpdateSurveyRequest;
import com.tlc.surveyexchange_backend.dto.response.*;
import com.tlc.surveyexchange_backend.exception.ResourceNotFoundException;
import com.tlc.surveyexchange_backend.exception.ForbiddenOperationException;
import com.tlc.surveyexchange_backend.exception.InvalidStatusTransitionException;
import com.tlc.surveyexchange_backend.model.Question;
import com.tlc.surveyexchange_backend.model.Survey;
import com.tlc.surveyexchange_backend.model.SurveyStatus;
import com.tlc.surveyexchange_backend.model.User;
import com.tlc.surveyexchange_backend.util.SurveyValueCalculator;
import com.tlc.surveyexchange_backend.repository.SurveyRepository;
import com.tlc.surveyexchange_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class SurveyService {

    private static final int PAGE_SIZE = 10;

    private final SurveyRepository surveyRepository;
    private final UserRepository userRepository;

    @Transactional
    public SurveyResponse createSurvey(CreateSurveyRequest request, UUID userId) {
        User owner = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        SurveyStatus status = (request.getStatus() == SurveyStatus.ACTIVE) ? SurveyStatus.ACTIVE : SurveyStatus.DRAFT;
        Survey survey = Survey.builder()
                .owner(owner)
                .title(request.getTitle())
                .description(request.getDescription())
                .category(request.getCategory())
                .estimatedTime(request.getEstimatedTime())
                .targetResponses(request.getTargetResponses())
                .status(status)
                .publishedAt(status == SurveyStatus.ACTIVE ? Instant.now() : null)
                .build();

        List<Question> questions = IntStream.range(0, request.getQuestions().size())
                .mapToObj(i -> {
                    var qr = request.getQuestions().get(i);
                    return Question.builder()
                            .survey(survey)
                            .text(qr.getText())
                            .questionType(qr.getQuestionType())
                            .options(qr.getOptions() != null ? qr.getOptions() : List.of())
                            .required(qr.isRequired())
                            .orderIndex(qr.getOrderIndex() > 0 ? qr.getOrderIndex() : i)
                            .build();
                })
                .toList();

        int creditValue = SurveyValueCalculator.calculate(
                request.getQuestions().stream().map(QuestionRequest::getQuestionType).toList());
        survey.setCreditValue(creditValue);
        survey.setQuestions(questions);
        Survey saved = surveyRepository.save(survey);
        return mapToSurveyResponse(saved);
    }

    @Transactional(readOnly = true)
    public PageResponse<SurveySummaryResponse> getFeed(UUID userId, int page) {
        Page<Survey> surveys = surveyRepository.findFeedForUser(userId, PageRequest.of(page, PAGE_SIZE));
        Page<SurveySummaryResponse> dtoPage = surveys.map(this::mapToSurveySummary);
        return PageResponse.from(dtoPage);
    }

    @Transactional(readOnly = true)
    public SurveyResponse getSurveyById(UUID id) {
        Survey survey = surveyRepository.findByIdWithQuestions(id)
                .orElseThrow(() -> new ResourceNotFoundException("Survey", id));
        return mapToSurveyResponse(survey);
    }

    @Transactional(readOnly = true)
    public List<SurveySummaryResponse> getMySurveys(UUID userId) {
        return surveyRepository.findByOwnerIdOrderByCreatedAtDesc(userId).stream()
                .map(this::mapToSurveySummary)
                .toList();
    }

    public Survey getSurveyEntityById(UUID id) {
        return surveyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Survey", id));
    }

    @Transactional
    public SurveyResponse updateSurvey(UUID surveyId, UUID requesterId, UpdateSurveyRequest request) {
        Survey survey = surveyRepository.findByIdWithQuestions(surveyId)
                .orElseThrow(() -> new ResourceNotFoundException("Survey", surveyId));

        if (!survey.getOwner().getId().equals(requesterId)) {
            throw new ForbiddenOperationException("Only the survey owner can edit this survey");
        }

        if (survey.getStatus() == SurveyStatus.CLOSED) {
            throw new ForbiddenOperationException("Cannot edit a closed survey");
        }

        survey.setTitle(request.getTitle());
        if (request.getDescription() != null) survey.setDescription(request.getDescription());
        if (request.getCategory() != null) survey.setCategory(request.getCategory());
        if (request.getEstimatedTime() != null) survey.setEstimatedTime(request.getEstimatedTime());
        if (request.getTargetResponses() != null) survey.setTargetResponses(request.getTargetResponses());

        if (request.getQuestions() != null) {
            survey.getQuestions().clear();
            List<Question> newQuestions = IntStream.range(0, request.getQuestions().size())
                    .mapToObj(i -> {
                        var qr = request.getQuestions().get(i);
                        return Question.builder()
                                .survey(survey)
                                .text(qr.getText())
                                .questionType(qr.getQuestionType())
                                .options(qr.getOptions() != null ? qr.getOptions() : List.of())
                                .required(qr.isRequired())
                                .orderIndex(qr.getOrderIndex() > 0 ? qr.getOrderIndex() : i)
                                .build();
                    })
                    .toList();
            survey.getQuestions().addAll(newQuestions);
            survey.setCreditValue(SurveyValueCalculator.calculate(
                    request.getQuestions().stream().map(QuestionRequest::getQuestionType).toList()));
        }

        return mapToSurveyResponse(surveyRepository.save(survey));
    }

    @Transactional
    public SurveySummaryResponse updateStatus(UUID surveyId, UUID requesterId, SurveyStatus newStatus) {
        Survey survey = surveyRepository.findById(surveyId)
                .orElseThrow(() -> new ResourceNotFoundException("Survey", surveyId));

        if (!survey.getOwner().getId().equals(requesterId)) {
            throw new ForbiddenOperationException("Only the survey owner can update this survey");
        }

        if (survey.getStatus() == newStatus) {
            throw new InvalidStatusTransitionException(
                    "Survey is already " + newStatus.name().toLowerCase().replace('_', '-'));
        }

        survey.setStatus(newStatus);
        if (newStatus == SurveyStatus.ACTIVE) {
            survey.setPublishedAt(Instant.now());
        } else if (newStatus == SurveyStatus.CLOSED) {
            survey.setClosedAt(Instant.now());
        }

        return mapToSurveySummary(surveyRepository.save(survey));
    }

    public SurveyResponse mapToSurveyResponse(Survey survey) {
        List<QuestionResponse> questions = survey.getQuestions().stream()
                .map(q -> QuestionResponse.builder()
                        .id(q.getId())
                        .text(q.getText())
                        .questionType(q.getQuestionType())
                        .options(q.getOptions())
                        .required(q.isRequired())
                        .orderIndex(q.getOrderIndex())
                        .build())
                .toList();

        return SurveyResponse.builder()
                .id(survey.getId())
                .title(survey.getTitle())
                .description(survey.getDescription())
                .category(survey.getCategory())
                .estimatedTime(survey.getEstimatedTime())
                .targetResponses(survey.getTargetResponses())
                .actualResponseCount(survey.getActualResponseCount())
                .status(survey.getStatus())
                .createdAt(survey.getCreatedAt())
                .boostExpiresAt(survey.getBoostExpiresAt())
                .boosted(survey.getBoostExpiresAt() != null && survey.getBoostExpiresAt().isAfter(Instant.now()))
                .creditValue(survey.getCreditValue())
                .owner(UserSummaryResponse.builder()
                        .id(survey.getOwner().getId())
                        .name(survey.getOwner().getFullName())
                        .email(survey.getOwner().getEmail())
                        .creditBalance(survey.getOwner().getCreditBalance())
                        .build())
                .questions(questions)
                .build();
    }

    private SurveySummaryResponse mapToSurveySummary(Survey survey) {
        return SurveySummaryResponse.builder()
                .id(survey.getId())
                .title(survey.getTitle())
                .category(survey.getCategory())
                .estimatedTime(survey.getEstimatedTime())
                .targetResponses(survey.getTargetResponses())
                .actualResponseCount(survey.getActualResponseCount())
                .status(survey.getStatus())
                .createdAt(survey.getCreatedAt())
                .publishedAt(survey.getPublishedAt())
                .boostExpiresAt(survey.getBoostExpiresAt())
                .boosted(survey.getBoostExpiresAt() != null && survey.getBoostExpiresAt().isAfter(Instant.now()))
                .creditValue(survey.getCreditValue())
                .owner(UserSummaryResponse.builder()
                        .id(survey.getOwner().getId())
                        .name(survey.getOwner().getFullName())
                        .email(survey.getOwner().getEmail())
                        .creditBalance(survey.getOwner().getCreditBalance())
                        .build())
                .build();
    }
}
