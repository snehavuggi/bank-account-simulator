package com.bank.model;

import java.math.BigDecimal;

/** Current: may go negative, up to an overdraft limit. */
public class CurrentAccount extends Account {

    public static final BigDecimal OVERDRAFT_LIMIT = new BigDecimal("1000.00");

    public CurrentAccount(String accountNumber, String holderName) {
        super(accountNumber, holderName);
    }

    @Override
    protected BigDecimal getMinimumAllowedBalance() {
        return OVERDRAFT_LIMIT.negate(); // -1000.00
    }

    @Override
    public String getAccountType() {
        return "CURRENT";
    }
}
