package com.nexabank.customer.dto;

public class AdminStatsResponse {
    private int totalUsers;
    private int totalCustomers;
    private int totalAdmins;
    private int totalEmployees;
    private int activeUsers;
    private int lockedUsers;
    private int totalAccounts;
    private double totalDeposits;
    private int totalTransactions;
    private double totalTransactionVolume;

    // Constructors
    public AdminStatsResponse() {}

    public AdminStatsResponse(int totalUsers, int totalCustomers, int totalAdmins, 
                             int totalEmployees, int activeUsers, int lockedUsers,
                             int totalAccounts, double totalDeposits, 
                             int totalTransactions, double totalTransactionVolume) {
        this.totalUsers = totalUsers;
        this.totalCustomers = totalCustomers;
        this.totalAdmins = totalAdmins;
        this.totalEmployees = totalEmployees;
        this.activeUsers = activeUsers;
        this.lockedUsers = lockedUsers;
        this.totalAccounts = totalAccounts;
        this.totalDeposits = totalDeposits;
        this.totalTransactions = totalTransactions;
        this.totalTransactionVolume = totalTransactionVolume;
    }

    // Getters and setters
    public int getTotalUsers() {
        return totalUsers;
    }

    public void setTotalUsers(int totalUsers) {
        this.totalUsers = totalUsers;
    }

    public int getTotalCustomers() {
        return totalCustomers;
    }

    public void setTotalCustomers(int totalCustomers) {
        this.totalCustomers = totalCustomers;
    }

    public int getTotalAdmins() {
        return totalAdmins;
    }

    public void setTotalAdmins(int totalAdmins) {
        this.totalAdmins = totalAdmins;
    }

    public int getTotalEmployees() {
        return totalEmployees;
    }

    public void setTotalEmployees(int totalEmployees) {
        this.totalEmployees = totalEmployees;
    }

    public int getActiveUsers() {
        return activeUsers;
    }

    public void setActiveUsers(int activeUsers) {
        this.activeUsers = activeUsers;
    }

    public int getLockedUsers() {
        return lockedUsers;
    }

    public void setLockedUsers(int lockedUsers) {
        this.lockedUsers = lockedUsers;
    }

    public int getTotalAccounts() {
        return totalAccounts;
    }

    public void setTotalAccounts(int totalAccounts) {
        this.totalAccounts = totalAccounts;
    }

    public double getTotalDeposits() {
        return totalDeposits;
    }

    public void setTotalDeposits(double totalDeposits) {
        this.totalDeposits = totalDeposits;
    }

    public int getTotalTransactions() {
        return totalTransactions;
    }

    public void setTotalTransactions(int totalTransactions) {
        this.totalTransactions = totalTransactions;
    }

    public double getTotalTransactionVolume() {
        return totalTransactionVolume;
    }

    public void setTotalTransactionVolume(double totalTransactionVolume) {
        this.totalTransactionVolume = totalTransactionVolume;
    }
}
