package com.bank.exception;

/** Thrown when a debit would take an account below its allowed minimum balance. */
public class InsufficientFundsException extends Exception {
    public InsufficientFundsException(String message) {
        super(message);
    }
}
