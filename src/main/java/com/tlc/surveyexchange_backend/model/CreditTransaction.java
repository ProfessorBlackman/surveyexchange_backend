package com.tlc.surveyexchange_backend.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Immutable;

import java.time.LocalDateTime;
import java.util.UUID;

@Immutable
@Entity
@Table(
        name = "credit_transactions",
        indexes = {
                @Index(name = "idx_credit_transactions_user_id", columnList = "user_id"),
                @Index(name = "idx_credit_transactions_created_at", columnList = "created_at")
        }
)
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreditTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private int amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;

    /** Refers to the survey ID for SURVEY_REWARD and BOOST_SPEND; null for SIGNUP_BONUS and PURCHASE. */
    @Column(name = "reference_id")
    private UUID referenceId;

    private String description;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
