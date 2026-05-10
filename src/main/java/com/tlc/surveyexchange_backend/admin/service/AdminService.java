package com.tlc.surveyexchange_backend.admin.service;

import com.tlc.surveyexchange_backend.exception.ResourceNotFoundException;
import com.tlc.surveyexchange_backend.model.*;
import com.tlc.surveyexchange_backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final SurveyRepository surveyRepository;
    private final ResponseRepository responseRepository;
    private final CreditTransactionRepository creditTransactionRepository;

    // ── Dashboard ─────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public DashboardStats getDashboardStats() {
        return new DashboardStats(
                userRepository.count(),
                surveyRepository.count(),
                responseRepository.count(),
                surveyRepository.countActiveBoosts(),
                userRepository.sumCreditBalance()
        );
    }

    @Transactional(readOnly = true)
    public List<DayCount> getResponsesLast7Days() {
        Instant since = Instant.now().minus(7, ChronoUnit.DAYS);
        List<Response> recent = responseRepository.findByRespondedAtAfter(since);

        Map<LocalDate, Long> byDay = recent.stream()
                .collect(Collectors.groupingBy(
                        r -> r.getRespondedAt().atZone(ZoneId.systemDefault()).toLocalDate(),
                        Collectors.counting()
                ));

        LocalDate today = LocalDate.now();
        return IntStream.rangeClosed(0, 6)
                .mapToObj(i -> {
                    LocalDate date = today.minusDays(6 - i);
                    return new DayCount(date, byDay.getOrDefault(date, 0L));
                })
                .toList();
    }

    // ── Users ─────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<User> getUsers(String search, int page, int size) {
        return userRepository.findForAdmin(search, PageRequest.of(page, size));
    }

    @Transactional(readOnly = true)
    public User getUserById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
    }

    @Transactional(readOnly = true)
    public long getUserSurveyCount(UUID userId) {
        return surveyRepository.countByOwnerId(userId);
    }

    @Transactional(readOnly = true)
    public long getUserResponseCount(UUID userId) {
        return responseRepository.countByRespondentId(userId);
    }

    @Transactional(readOnly = true)
    public List<CreditTransaction> getUserTransactions(UUID userId) {
        return creditTransactionRepository
                .findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, 20))
                .getContent();
    }

    // ── Surveys ───────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<Survey> getSurveys(SurveyStatus status, String search, int page, int size) {
        return surveyRepository.findForAdmin(status, search, PageRequest.of(page, size));
    }

    @Transactional(readOnly = true)
    public Survey getSurveyById(UUID id) {
        return surveyRepository.findByIdWithQuestions(id)
                .orElseThrow(() -> new ResourceNotFoundException("Survey", id));
    }

    @Transactional
    public void disableSurvey(UUID id) {
        Survey survey = surveyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Survey", id));
        survey.setStatus(SurveyStatus.CLOSED);
        survey.setClosedAt(Instant.now());
        surveyRepository.save(survey);
    }

    @Transactional
    public void deleteSurvey(UUID id) {
        Survey survey = surveyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Survey", id));
        List<Response> responses = responseRepository.findAllBySurveyId(id);
        responseRepository.deleteAll(responses);
        surveyRepository.delete(survey);
    }

    // ── Transactions ──────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<CreditTransaction> getTransactions(TransactionType type, int page, int size) {
        PageRequest pr = PageRequest.of(page, size);
        return type != null
                ? creditTransactionRepository.findByTypeOrderByCreatedAtDesc(type, pr)
                : creditTransactionRepository.findAllByOrderByCreatedAtDesc(pr);
    }

    // ── Inner types ───────────────────────────────────────────────────────────

    public record DashboardStats(long totalUsers, long totalSurveys, long totalResponses,
                                  long activeBoosts, long creditsInCirculation) {}

    public record DayCount(LocalDate date, long count) {}
}
