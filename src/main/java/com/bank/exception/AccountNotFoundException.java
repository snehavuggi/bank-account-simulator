package com.bank.exception;

/** Thrown when an account number does not exist in the bank. */
public class AccountNotFoundException extends Exception {
    public AccountNotFoundException(String accountNumber) {
        super("Account not found: " + accountNumber);
    }
}
