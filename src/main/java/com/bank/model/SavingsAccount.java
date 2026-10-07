package com.bank.model;

import com.bank.exception.InvalidAmountException;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Savings: must always keep a minimum balance, and earns interest. */
public class SavingsAccount extends Account {

    public static final BigDecimal MINIMUM_BALANCE = new BigDecimal("500.00");
    public static final BigDecimal ANNUAL_INTEREST_RATE = new BigDecimal("0.04"); // 4%

    public SavingsAccount(String accountNumber, String holderName) {
        super(accountNumber, holderName);
    }

    @Override
    protected BigDecimal getMinimumAllowedBalance() {
        return MINIMUM_BALANCE;
    }

    @Override
    public String getAccountType() {
        return "SAVINGS";
    }

    /** Credits one month of interest on the current balance. */
    public void applyMonthlyInterest() throws InvalidAmountException {
        lock(); // balance read + credit should be one atomic step
        try {
            BigDecimal interest = getBalance()
                    .multiply(ANNUAL_INTEREST_RATE)
                    .divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);
            if (interest.signum() > 0) {
                credit(interest, TransactionType.INTEREST, "Monthly interest");
            }
        } finally {
            unlock();
        }
    }
}
