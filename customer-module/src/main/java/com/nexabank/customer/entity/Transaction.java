package com.nexabank.customer.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "transaction_id", updatable = false, nullable = false)
    private String transactionId;
    
    @Column(name = "account_id", nullable = false)
    private String accountId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false)
    private TransactionType transactionType;
    
    @Column(name = "amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal amount;
    
    @Column(name = "description")
    private String description;
    
    @Column(name = "balance_after", precision = 15, scale = 2)
    private BigDecimal balanceAfter;
    
    @Column(name = "reference_number")
    private String referenceNumber;
    
    @Column(name = "target_account_id")
    private String targetAccountId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private TransactionStatus status = TransactionStatus.PENDING;
    
    @Column(name = "category")
    private String category;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "processed_at")
    private LocalDateTime processedAt;
    
    public enum TransactionType {
        CREDIT, DEBIT, TRANSFER_IN, TRANSFER_OUT
    }
    
    public enum TransactionStatus {
        PENDING, COMPLETED, FAILED, CANCELLED
    }
}
