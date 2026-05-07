package com.tlc.surveyexchange_backend.service;

import com.tlc.surveyexchange_backend.dto.request.PurchaseRequest;
import com.tlc.surveyexchange_backend.dto.response.CreditBalanceResponse;
import com.tlc.surveyexchange_backend.dto.response.CreditTransactionResponse;
import com.tlc.surveyexchange_backend.dto.response.PageResponse;
import com.tlc.surveyexchange_backend.dto.response.PurchaseResponse;
import com.tlc.surveyexchange_backend.exception.ResourceNotFoundException;
import com.tlc.surveyexchange_backend.model.CreditTransaction;
import com.tlc.surveyexchange_backend.model.TransactionType;
import com.tlc.surveyexchange_backend.model.User;
import com.tlc.surveyexchange_backend.repository.CreditTransactionRepository;
import com.tlc.surveyexchange_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CreditService {

    private static final int PAGE_SIZE = 10;
    private static final Set<Integer> VALID_PACKAGES = Set.of(10, 25, 50, 100);

    private final CreditTransactionRepository creditTransactionRepository;
    private final UserRepository userRepository;

    @Transactional
    public PurchaseResponse purchaseCredits(UUID userId, PurchaseRequest request) {
        if (!VALID_PACKAGES.contains(request.getCreditPackage())) {
            throw new IllegalArgumentException(
                    "Invalid credit package. Valid options: " + VALID_PACKAGES);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        int creditsAdded = request.getCreditPackage();

        // Mock payment — always succeeds in v1
        user.setCreditBalance(user.getCreditBalance() + creditsAdded);
        userRepository.save(user);

        CreditTransaction transaction = CreditTransaction.builder()
                .user(user)
                .amount(creditsAdded)
                .type(TransactionType.PURCHASE)
                .description("Purchased " + creditsAdded + " credits")
                .build();
        creditTransactionRepository.save(transaction);

        return PurchaseResponse.builder()
                .creditsAdded(creditsAdded)
                .newBalance(user.getCreditBalance())
                .transaction(toResponse(transaction))
                .build();
    }

    @Transactional(readOnly = true)
    public CreditBalanceResponse getCreditBalance(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        return new CreditBalanceResponse(user.getCreditBalance());
    }

    @Transactional(readOnly = true)
    public PageResponse<CreditTransactionResponse> getCreditHistory(UUID userId, int page) {
        Page<CreditTransaction> transactions = creditTransactionRepository
                .findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, PAGE_SIZE));

        return PageResponse.from(transactions.map(this::toResponse));
    }

    private CreditTransactionResponse toResponse(CreditTransaction t) {
        return CreditTransactionResponse.builder()
                .id(t.getId())
                .amount(t.getAmount())
                .type(t.getType())
                .description(t.getDescription())
                .referenceId(t.getReferenceId())
                .createdAt(t.getCreatedAt())
                .build();
    }
}
