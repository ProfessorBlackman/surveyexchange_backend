package com.tlc.surveyexchange_backend.service;

import com.tlc.surveyexchange_backend.dto.request.SubmitResponseRequest;
import com.tlc.surveyexchange_backend.exception.DuplicateResourceException;
import com.tlc.surveyexchange_backend.exception.ForbiddenOperationException;
import com.tlc.surveyexchange_backend.exception.ResourceNotFoundException;
import com.tlc.surveyexchange_backend.model.*;
import com.tlc.surveyexchange_backend.repository.CreditTransactionRepository;
import com.tlc.surveyexchange_backend.repository.QuestionRepository;
import com.tlc.surveyexchange_backend.repository.ResponseRepository;
import com.tlc.surveyexchange_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ResponseService {

    private final ResponseRepository responseRepository;
    private final UserRepository userRepository;
    private final QuestionRepository questionRepository;
    private final CreditTransactionRepository creditTransactionRepository;
    private final SurveyService surveyService;

    @Transactional
    public void submitResponse(UUID surveyId, SubmitResponseRequest request, UUID userId) {
        Survey survey = surveyService.getSurveyEntityById(surveyId);

        if (survey.getOwner().getId().equals(userId)) {
            throw new ForbiddenOperationException("You cannot respond to your own survey");
        }

        if (responseRepository.existsByUserIdAndSurveyId(userId, surveyId)) {
            throw new DuplicateResourceException("You have already responded to this survey");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        Response response = Response.builder()
                .user(user)
                .survey(survey)
                .build();

        List<Answer> answers = request.getAnswers().stream()
                .map(ar -> {
                    Question question = questionRepository.findById(ar.getQuestionId())
                            .orElseThrow(() -> new ResourceNotFoundException("Question", ar.getQuestionId()));
                    return Answer.builder()
                            .response(response)
                            .question(question)
                            .answerText(ar.getAnswerText())
                            .selectedOptions(ar.getSelectedOptions() != null ? ar.getSelectedOptions() : List.of())
                            .build();
                })
                .toList();

        response.setAnswers(answers);
        responseRepository.save(response);

        user.setCreditBalance(user.getCreditBalance() + 1);
        userRepository.save(user);

        CreditTransaction transaction = CreditTransaction.builder()
                .user(user)
                .amount(survey.getCreditValue())
                .type(TransactionType.SURVEY_REWARD)
                .referenceId(survey.getId())
                .description("Credit earned for completing survey")
                .build();
        creditTransactionRepository.save(transaction);
    }
}
