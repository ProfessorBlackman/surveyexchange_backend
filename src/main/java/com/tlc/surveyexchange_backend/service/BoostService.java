package com.tlc.surveyexchange_backend.service;

import com.tlc.surveyexchange_backend.constants.BoostConstants;
import com.tlc.surveyexchange_backend.exception.AlreadyBoostedException;
import com.tlc.surveyexchange_backend.exception.ForbiddenOperationException;
import com.tlc.surveyexchange_backend.exception.InsufficientCreditsException;
import com.tlc.surveyexchange_backend.exception.ResourceNotFoundException;
import com.tlc.surveyexchange_backend.model.CreditTransaction;
import com.tlc.surveyexchange_backend.model.Survey;
import com.tlc.surveyexchange_backend.model.TransactionType;
import com.tlc.surveyexchange_backend.model.User;
import com.tlc.surveyexchange_backend.repository.CreditTransactionRepository;
import com.tlc.surveyexchange_backend.repository.SurveyRepository;
import com.tlc.surveyexchange_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BoostService {

    private final UserRepository userRepository;
    private final SurveyRepository surveyRepository;
    private final CreditTransactionRepository creditTransactionRepository;

    @Transactional
    public BoostResult boost(UUID userId, UUID surveyId) {
        Survey survey = surveyRepository.findById(surveyId)
                .orElseThrow(() -> new ResourceNotFoundException("Survey", surveyId));

        if (!survey.getOwner().getId().equals(userId)) {
            throw new ForbiddenOperationException("You do not own this survey");
        }

        if (survey.getBoostExpiresAt() != null && survey.getBoostExpiresAt().isAfter(Instant.now())) {
            throw new AlreadyBoostedException(survey.getBoostExpiresAt());
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        if (user.getCreditBalance() < BoostConstants.BOOST_MIN_BALANCE) {
            throw new InsufficientCreditsException(user.getCreditBalance());
        }

        user.setCreditBalance(user.getCreditBalance() - BoostConstants.BOOST_COST);
        userRepository.save(user);

        CreditTransaction transaction = CreditTransaction.builder()
                .user(user)
                .amount(-BoostConstants.BOOST_COST)
                .type(TransactionType.BOOST_SPEND)
                .referenceId(survey.getId())
                .description("Survey boost for 24 hours")
                .build();
        creditTransactionRepository.save(transaction);

        Instant expiresAt = Instant.now().plus(BoostConstants.BOOST_DURATION_HOURS, ChronoUnit.HOURS);
        survey.setBoostExpiresAt(expiresAt);
        surveyRepository.save(survey);

        return new BoostResult(user.getCreditBalance(), expiresAt);
    }
}
