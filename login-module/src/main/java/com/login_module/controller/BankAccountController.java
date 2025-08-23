package com.login_module.controller;

import com.login_module.dto.ApiResponse;
import com.login_module.entity.BankAccount;
import com.login_module.entity.Transaction;
import com.login_module.service.BankAccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
@CrossOrigin(origins = "*", maxAge = 3600)
public class BankAccountController {
    
    @Autowired
    private BankAccountService bankAccountService;
    
    @GetMapping("/my-accounts")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN') or hasRole('EMPLOYEE')")
    public ResponseEntity<ApiResponse<List<BankAccount>>> getUserAccounts(Authentication authentication) {
        try {
            String userId = ((com.login_module.service.UserDetailsServiceImpl.UserPrincipal) authentication.getPrincipal()).getId();
            List<BankAccount> accounts = bankAccountService.getUserAccounts(userId);
            return ResponseEntity.ok(ApiResponse.success("Accounts retrieved successfully!", accounts));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @GetMapping("/my-transactions")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN') or hasRole('EMPLOYEE')")
    public ResponseEntity<ApiResponse<List<Transaction>>> getUserTransactions(
            Authentication authentication,
            @RequestParam(defaultValue = "10") int limit) {
        try {
            String userId = ((com.login_module.service.UserDetailsServiceImpl.UserPrincipal) authentication.getPrincipal()).getId();
            List<Transaction> transactions = bankAccountService.getUserTransactions(userId, limit);
            return ResponseEntity.ok(ApiResponse.success("Transactions retrieved successfully!", transactions));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @PostMapping("/create")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN') or hasRole('EMPLOYEE')")
    public ResponseEntity<ApiResponse<BankAccount>> createAccount(
            Authentication authentication,
            @RequestParam BankAccount.AccountType accountType) {
        try {
            String userId = ((com.login_module.service.UserDetailsServiceImpl.UserPrincipal) authentication.getPrincipal()).getId();
            BankAccount account = bankAccountService.createAccount(userId, accountType);
            return ResponseEntity.ok(ApiResponse.success("Account created successfully!", account));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @GetMapping("/{accountId}/transactions")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN') or hasRole('EMPLOYEE')")
    public ResponseEntity<ApiResponse<List<Transaction>>> getAccountTransactions(
            @PathVariable String accountId,
            @RequestParam(defaultValue = "20") int limit) {
        try {
            List<Transaction> transactions = bankAccountService.getAccountTransactions(accountId, limit);
            return ResponseEntity.ok(ApiResponse.success("Account transactions retrieved successfully!", transactions));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
}
