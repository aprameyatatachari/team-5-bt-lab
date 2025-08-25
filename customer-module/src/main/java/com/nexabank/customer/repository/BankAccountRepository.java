package com.nexabank.customer.repository;

import com.nexabank.customer.entity.BankAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BankAccountRepository extends JpaRepository<BankAccount, String> {
    
    @Query("SELECT COALESCE(SUM(ba.balance), 0) FROM BankAccount ba")
    double getTotalDeposits();
    
    @Query("SELECT COUNT(ba) FROM BankAccount ba")
    long count();
    
    List<BankAccount> findByAccountType(BankAccount.AccountType accountType);
    
    List<BankAccount> findByStatus(BankAccount.AccountStatus status);
    
    List<BankAccount> findByAccountTypeAndStatus(BankAccount.AccountType accountType, BankAccount.AccountStatus status);
    
    Optional<BankAccount> findById(String accountId);
}
