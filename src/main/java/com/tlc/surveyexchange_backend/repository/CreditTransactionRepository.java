package com.tlc.surveyexchange_backend.repository;

import com.tlc.surveyexchange_backend.model.CreditTransaction;
import com.tlc.surveyexchange_backend.model.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CreditTransactionRepository extends JpaRepository<CreditTransaction, UUID> {

    Page<CreditTransaction> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    Page<CreditTransaction> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<CreditTransaction> findByTypeOrderByCreatedAtDesc(TransactionType type, Pageable pageable);
}
