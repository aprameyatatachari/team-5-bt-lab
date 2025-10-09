package com.nexabank.customer.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "bank_accounts")
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
public class BankAccount extends AuditLoggable {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "account_id", updatable = false, nullable = false)
    private String accountId;
    
    @Column(name = "account_number", unique = true, nullable = false)
    private String accountNumber;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false)
    private AccountType accountType;
    
    @Column(name = "balance", precision = 15, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;
    
    // Foreign key to Customer entity
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private AccountStatus status = AccountStatus.ACTIVE;
    
    @Column(name = "interest_rate", precision = 5, scale = 2)
    private BigDecimal interestRate;
    
    @Column(name = "branch_code")
    private String branchCode;
    
    @Column(name = "ifsc_code")
    private String ifscCode;
    
    @Column(name = "minimum_balance", precision = 15, scale = 2)
    private BigDecimal minimumBalance = BigDecimal.ZERO;
    
    @Column(name = "daily_withdrawal_limit", precision = 15, scale = 2)
    private BigDecimal dailyWithdrawalLimit;
    
    @Column(name = "monthly_transaction_limit", precision = 15, scale = 2)
    private BigDecimal monthlyTransactionLimit;
    
    @Column(name = "last_transaction_date")
    private LocalDateTime lastTransactionDate;
    
    @Column(name = "account_opening_date")
    private LocalDateTime accountOpeningDate = LocalDateTime.now();
    
    public enum AccountType {
        SAVINGS, CURRENT, FIXED_DEPOSIT, LOAN
    }
    
    public enum AccountStatus {
        ACTIVE, INACTIVE, SUSPENDED, CLOSED
    }
}
