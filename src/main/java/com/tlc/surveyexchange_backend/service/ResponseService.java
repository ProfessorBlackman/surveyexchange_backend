package com.tlc.surveyexchange_backend.service;

import com.tlc.surveyexchange_backend.dto.request.SubmitResponseRequest;
import com.tlc.surveyexchange_backend.exception.DuplicateResourceException;
import com.tlc.surveyexchange_backend.exception.ForbiddenOperationException;
import com.tlc.surveyexchange_backend.exception.ResourceNotFoundException;
import com.tlc.surveyexchange_backend.model.*;
import com.tlc.surveyexchange_backend.repository.CreditTransactionRepository;
import com.tlc.surveyexchange_backend.repository.QuestionRepository;
import com.tlc.surveyexchange_backend.repository.ResponseRepository;
import com.tlc.surveyexchange_backend.repository.SurveyRepository;
import com.tlc.surveyexchange_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ResponseService {

    private final ResponseRepository responseRepository;
    private final UserRepository userRepository;
    private final QuestionRepository questionRepository;
    private final CreditTransactionRepository creditTransactionRepository;
    private final SurveyRepository surveyRepository;
    private final SurveyService surveyService;

    @Transactional
    public int submitResponse(UUID surveyId, SubmitResponseRequest request, UUID userId) {
        Survey survey = surveyService.getSurveyEntityById(surveyId);

        if (survey.getOwner().getId().equals(userId)) {
            throw new ForbiddenOperationException("You cannot respond to your own survey");
        }

        if (responseRepository.existsByRespondentIdAndSurveyId(userId, surveyId)) {
            throw new DuplicateResourceException("You have already responded to this survey");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        Response response = Response.builder()
                .respondent(user)
                .survey(survey)
                .timeToCompleteSeconds(capCompletionTime(request.getTimeToCompleteSeconds()))
                .respondentAcademicLevel(user.getAcademicLevel())
                .respondentFieldOfStudy(user.getFieldOfStudy())
                .respondentInstitution(user.getInstitution())
                .build();

        List<Answer> answers = request.getAnswers().stream()
                .map(ar -> {
                    Question question = questionRepository.findById(ar.getQuestionId())
                            .orElseThrow(() -> new ResourceNotFoundException("Question", ar.getQuestionId()));
                    return Answer.builder()
                            .response(response)
                            .question(question)
                            .answerValue(ar.getAnswerValue() != null ? ar.getAnswerValue() : "")
                            .build();
                })
                .toList();

        response.setAnswers(answers);
        responseRepository.save(response);

        survey.setActualResponseCount(survey.getActualResponseCount() + 1);
        if (survey.getTargetResponses() != null
                && survey.getActualResponseCount() >= survey.getTargetResponses()) {
            survey.setStatus(SurveyStatus.CLOSED);
            survey.setClosedAt(Instant.now());
        }
        surveyRepository.save(survey);

        user.setCreditBalance(user.getCreditBalance() + survey.getCreditValue());
        userRepository.save(user);

        CreditTransaction transaction = CreditTransaction.builder()
                .user(user)
                .amount(survey.getCreditValue())
                .type(TransactionType.SURVEY_REWARD)
                .referenceId(survey.getId())
                .description("Credit earned for completing survey")
                .build();
        creditTransactionRepository.save(transaction);

        return user.getCreditBalance();
    }

    private Integer capCompletionTime(Integer rawSeconds) {
        if (rawSeconds == null) return null;
        return Math.max(Math.min(rawSeconds, 7200), 0);
    }
}
