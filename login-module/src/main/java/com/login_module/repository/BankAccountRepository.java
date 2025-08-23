package com.login_module.repository;

import com.login_module.entity.BankAccount;
import com.login_module.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BankAccountRepository extends JpaRepository<BankAccount, String> {
    
    List<BankAccount> findByUser(User user);
    
    List<BankAccount> findByUserAndStatus(User user, BankAccount.AccountStatus status);
    
    boolean existsByAccountNumber(String accountNumber);
    
    @Query("SELECT COUNT(a) FROM BankAccount a WHERE a.accountType = :accountType")
    Long countByAccountType(@Param("accountType") BankAccount.AccountType accountType);
    
    @Query("SELECT SUM(a.balance) FROM BankAccount a WHERE a.status = 'ACTIVE'")
    Double getTotalBalance();
}
