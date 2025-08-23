package com.login_module.repository;

import com.login_module.entity.BankAccount;
import com.login_module.entity.Transaction;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, String> {
    
    List<Transaction> findByAccountOrderByCreatedAtDesc(BankAccount account);
    
    List<Transaction> findByAccountOrderByCreatedAtDesc(BankAccount account, Pageable pageable);
    
    @Query("SELECT t FROM Transaction t WHERE t.account.user.userId = :userId ORDER BY t.createdAt DESC")
    List<Transaction> findByUserIdOrderByCreatedAtDesc(@Param("userId") String userId, Pageable pageable);
    
    @Query("SELECT t FROM Transaction t WHERE t.account = :account AND t.createdAt BETWEEN :startDate AND :endDate ORDER BY t.createdAt DESC")
    List<Transaction> findByAccountAndDateRange(@Param("account") BankAccount account, 
                                               @Param("startDate") LocalDateTime startDate, 
                                               @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT t FROM Transaction t WHERE t.account = :account AND t.transactionType = :type ORDER BY t.createdAt DESC")
    List<Transaction> findByAccountAndTransactionType(@Param("account") BankAccount account, 
                                                     @Param("type") Transaction.TransactionType type, 
                                                     Pageable pageable);
}
