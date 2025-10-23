package com.nexabank.customer.service;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Generates unique customer numbers for the bank
 * Format: CUST-YYYYMMDD-XXXXXX
 * Example: CUST-20251023-000001
 */
@Service
public class CustomerNumberGenerator {
    
    private static final String PREFIX = "CUST";
    private static final AtomicInteger counter = new AtomicInteger(0);
    private static String lastDate = "";
    
    /**
     * Generate a unique customer number
     * This number stays the same across all versions of the customer record
     */
    public synchronized String generateCustomerNumber() {
        LocalDateTime now = LocalDateTime.now();
        String currentDate = now.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        
        // Reset counter if date changes
        if (!currentDate.equals(lastDate)) {
            counter.set(0);
            lastDate = currentDate;
        }
        
        int sequence = counter.incrementAndGet();
        
        return String.format("%s-%s-%06d", PREFIX, currentDate, sequence);
    }
}
