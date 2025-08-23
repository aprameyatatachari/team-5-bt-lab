package com.login_module.service;

import com.login_module.entity.BankAccount;
import com.login_module.entity.Transaction;
import com.login_module.entity.User;
import com.login_module.exception.BadRequestException;
import com.login_module.exception.UnauthorizedException;
import com.login_module.repository.BankAccountRepository;
import com.login_module.repository.TransactionRepository;
import com.login_module.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

@Service
public class BankAccountService {
    
    @Autowired
    private BankAccountRepository bankAccountRepository;
    
    @Autowired
    private TransactionRepository transactionRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    public List<BankAccount> getUserAccounts(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("User not found"));
        return bankAccountRepository.findByUserAndStatus(user, BankAccount.AccountStatus.ACTIVE);
    }
    
    public List<Transaction> getUserTransactions(String userId, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return transactionRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }
    
    public List<Transaction> getAccountTransactions(String accountId, int limit) {
        BankAccount account = bankAccountRepository.findById(accountId)
                .orElseThrow(() -> new BadRequestException("Account not found"));
        Pageable pageable = PageRequest.of(0, limit);
        return transactionRepository.findByAccountOrderByCreatedAtDesc(account, pageable);
    }
    
    @Transactional
    public BankAccount createAccount(String userId, BankAccount.AccountType accountType) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("User not found"));
        
        BankAccount account = new BankAccount();
        account.setUser(user);
        account.setAccountType(accountType);
        account.setAccountNumber(generateAccountNumber());
        account.setStatus(BankAccount.AccountStatus.ACTIVE);
        
        // Set interest rates based on account type
        switch (accountType) {
            case SAVINGS:
                account.setInterestRate(new BigDecimal("4.5"));
                break;
            case CURRENT:
                account.setInterestRate(new BigDecimal("2.0"));
                break;
            case FIXED_DEPOSIT:
                account.setInterestRate(new BigDecimal("6.5"));
                break;
            default:
                account.setInterestRate(new BigDecimal("0.0"));
        }
        
        return bankAccountRepository.save(account);
    }
    
    @Transactional
    public Transaction createTransaction(String accountId, Transaction.TransactionType type, 
                                       BigDecimal amount, String description, String category) {
        BankAccount account = bankAccountRepository.findById(accountId)
                .orElseThrow(() -> new BadRequestException("Account not found"));
        
        Transaction transaction = new Transaction();
        transaction.setAccount(account);
        transaction.setTransactionType(type);
        transaction.setAmount(amount);
        transaction.setDescription(description);
        transaction.setCategory(category);
        transaction.setReferenceNumber(generateReferenceNumber());
        transaction.setStatus(Transaction.TransactionStatus.COMPLETED);
        transaction.setProcessedAt(LocalDateTime.now());
        
        // Update account balance
        BigDecimal newBalance;
        if (type == Transaction.TransactionType.CREDIT || type == Transaction.TransactionType.TRANSFER_IN) {
            newBalance = account.getBalance().add(amount);
        } else {
            newBalance = account.getBalance().subtract(amount);
            if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
                throw new BadRequestException("Insufficient balance");
            }
        }
        
        account.setBalance(newBalance);
        account.setLastTransactionDate(LocalDateTime.now());
        transaction.setBalanceAfter(newBalance);
        
        bankAccountRepository.save(account);
        return transactionRepository.save(transaction);
    }
    
    private String generateAccountNumber() {
        String accountNumber;
        do {
            accountNumber = String.format("%010d", new Random().nextInt(1000000000));
        } while (bankAccountRepository.existsByAccountNumber(accountNumber));
        return accountNumber;
    }
    
    private String generateReferenceNumber() {
        return "TXN" + System.currentTimeMillis() + new Random().nextInt(1000);
    }
}
