-- Insert sample bank accounts for existing users
INSERT INTO bank_accounts (account_id, account_number, account_type, balance, user_id, interest_rate, created_at, last_transaction_date)
VALUES 
    (UUID(), '1234567890', 'SAVINGS', 125000.00, (SELECT user_id FROM users WHERE email = 'aprameya.tatachari@gmail.com'), 4.50, NOW(), NOW()),
    (UUID(), '1234567891', 'CURRENT', 75000.00, (SELECT user_id FROM users WHERE email = 'aprameya.tatachari@gmail.com'), 2.00, NOW(), NOW()),
    (UUID(), '1234567892', 'FIXED_DEPOSIT', 500000.00, (SELECT user_id FROM users WHERE email = 'admin1@nexabank.com'), 6.50, NOW(), NOW());

-- Insert sample transactions
INSERT INTO transactions (transaction_id, account_id, transaction_type, amount, description, balance_after, reference_number, status, category, created_at, processed_at)
VALUES 
    (UUID(), (SELECT account_id FROM bank_accounts WHERE account_number = '1234567890'), 'CREDIT', 5000.00, 'Salary Credit', 125000.00, 'TXN123456', 'COMPLETED', 'Income', DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY)),
    (UUID(), (SELECT account_id FROM bank_accounts WHERE account_number = '1234567890'), 'DEBIT', 1200.00, 'Online Purchase - Amazon', 120000.00, 'TXN123457', 'COMPLETED', 'Shopping', DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY)),
    (UUID(), (SELECT account_id FROM bank_accounts WHERE account_number = '1234567890'), 'DEBIT', 500.00, 'ATM Withdrawal', 121200.00, 'TXN123458', 'COMPLETED', 'Cash', DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY)),
    (UUID(), (SELECT account_id FROM bank_accounts WHERE account_number = '1234567890'), 'CREDIT', 2500.00, 'Money Transfer from Friend', 121700.00, 'TXN123459', 'COMPLETED', 'Transfer', DATE_SUB(NOW(), INTERVAL 4 DAY), DATE_SUB(NOW(), INTERVAL 4 DAY)),
    (UUID(), (SELECT account_id FROM bank_accounts WHERE account_number = '1234567890'), 'DEBIT', 850.00, 'Electricity Bill Payment', 119200.00, 'TXN123460', 'COMPLETED', 'Utilities', DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY));
