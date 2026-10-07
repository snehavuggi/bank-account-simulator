package com.bank.service;

import com.bank.exception.AccountNotFoundException;
import com.bank.exception.InsufficientFundsException;
import com.bank.exception.InvalidAmountException;
import com.bank.model.Account;
import com.bank.model.CurrentAccount;
import com.bank.model.SavingsAccount;
import com.bank.model.TransactionType;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/** Creates accounts, finds them, and performs transfers between them. */
public class Bank {

    // ConcurrentHashMap: safe for many threads adding/reading accounts at once.
    private final Map<String, Account> accounts = new ConcurrentHashMap<>();
    private final AtomicInteger nextNumber = new AtomicInteger(1000);

    public SavingsAccount createSavingsAccount(String holderName, BigDecimal initialDeposit)
            throws InvalidAmountException {
        SavingsAccount account = new SavingsAccount(newAccountNumber(), holderName);
        fund(account, initialDeposit);
        accounts.put(account.getAccountNumber(), account);
        return account;
    }

    public CurrentAccount createCurrentAccount(String holderName, BigDecimal initialDeposit)
            throws InvalidAmountException {
        CurrentAccount account = new CurrentAccount(newAccountNumber(), holderName);
        fund(account, initialDeposit);
        accounts.put(account.getAccountNumber(), account);
        return account;
    }

    public Account getAccount(String accountNumber) throws AccountNotFoundException {
        Account account = accounts.get(accountNumber);
        if (account == null) {
            throw new AccountNotFoundException(accountNumber);
        }
        return account;
    }

    public Collection<Account> getAllAccounts() {
        return accounts.values();
    }

    /**
     * Moves money from one account to another, all-or-nothing.
     *
     * Atomic: we lock BOTH accounts, so nobody can observe the money "in flight".
     * Deadlock-free: locks are always taken in account-number order. Without this,
     * A->B and B->A running at the same time could each hold one lock and wait
     * forever for the other.
     */
    public void transfer(String fromNumber, String toNumber, BigDecimal amount)
            throws AccountNotFoundException, InvalidAmountException, InsufficientFundsException {

        if (fromNumber.equals(toNumber)) {
            throw new IllegalArgumentException("Cannot transfer to the same account");
        }

        Account from = getAccount(fromNumber);
        Account to = getAccount(toNumber);

        // Consistent lock ordering
        Account first = fromNumber.compareTo(toNumber) < 0 ? from : to;
        Account second = (first == from) ? to : from;

        first.lock();
        try {
            second.lock();
            try {
                // debit first: if it fails (no funds / bad amount), nothing has changed yet
                from.debit(amount, TransactionType.TRANSFER_OUT, "Transfer to " + toNumber);
                to.credit(amount, TransactionType.TRANSFER_IN, "Transfer from " + fromNumber);
            } finally {
                second.unlock();
            }
        } finally {
            first.unlock();
        }
    }

    private String newAccountNumber() {
        return "ACC" + nextNumber.incrementAndGet();
    }

    private void fund(Account account, BigDecimal initialDeposit) throws InvalidAmountException {
        if (initialDeposit == null || initialDeposit.signum() < 0) {
            throw new InvalidAmountException("Initial deposit cannot be negative");
        }
        if (initialDeposit.signum() > 0) {
            account.deposit(initialDeposit);
        }
    }
}
