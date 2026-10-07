package com.bank.exception;

/** Thrown when an amount is null, zero, negative, or rounds to zero. */
public class InvalidAmountException extends Exception {
    public InvalidAmountException(String message) {
        super(message);
    }
}
