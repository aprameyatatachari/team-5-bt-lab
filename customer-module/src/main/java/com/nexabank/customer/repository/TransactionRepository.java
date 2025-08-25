package com.nexabank.customer.repository;

import com.nexabank.customer.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t")
    double getTotalTransactionVolume();
    
    @Query("SELECT COUNT(t) FROM Transaction t")
    long count();
}
