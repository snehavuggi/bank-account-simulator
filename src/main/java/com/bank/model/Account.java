package com.bank.model;

import com.bank.exception.InsufficientFundsException;
import com.bank.exception.InvalidAmountException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Base class for all accounts.
 *
 * Encapsulation: balance and transactions are private. The only way to change
 * them is through credit()/debit(), which validate input first.
 *
 * Thread-safety: every read or write of the balance happens while holding this
 * account's ReentrantLock.
 */
public abstract class Account {

    private final String accountNumber;
    private final String holderName;
    private BigDecimal balance = BigDecimal.ZERO.setScale(2);
    private final List<Transaction> transactions = new ArrayList<>();
    private final ReentrantLock lock = new ReentrantLock();

    protected Account(String accountNumber, String holderName) {
        this.accountNumber = accountNumber;
        this.holderName = holderName;
    }

    /** Each account type decides the lowest balance it may reach (polymorphism). */
    protected abstract BigDecimal getMinimumAllowedBalance();

    public abstract String getAccountType();

    // ---------- public operations ----------

    public void deposit(BigDecimal amount) throws InvalidAmountException {
        credit(amount, TransactionType.DEPOSIT, "Cash deposit");
    }

    public void withdraw(BigDecimal amount) throws InvalidAmountException, InsufficientFundsException {
        debit(amount, TransactionType.WITHDRAWAL, "Cash withdrawal");
    }

    /** Adds money. Used by deposit(), transfers and interest. */
    public void credit(BigDecimal amount, TransactionType type, String description)
            throws InvalidAmountException {
        BigDecimal value = validate(amount);
        lock.lock();
        try {
            balance = balance.add(value);
            transactions.add(new Transaction(type, value, balance, description));
        } finally {
            lock.unlock(); // always release, even if an exception is thrown
        }
    }

    /**
     * Removes money. The balance check and the update happen inside the same
     * lock, so no other thread can sneak in between "check" and "act".
     */
    public void debit(BigDecimal amount, TransactionType type, String description)
            throws InvalidAmountException, InsufficientFundsException {
        BigDecimal value = validate(amount);
        lock.lock();
        try {
            BigDecimal newBalance = balance.subtract(value);
            if (newBalance.compareTo(getMinimumAllowedBalance()) < 0) {
                throw new InsufficientFundsException(String.format(
                        "Insufficient funds in %s: balance %s, requested %s, minimum allowed %s",
                        accountNumber, balance, value, getMinimumAllowedBalance()));
            }
            balance = newBalance;
            transactions.add(new Transaction(type, value, balance, description));
        } finally {
            lock.unlock();
        }
    }

    // ---------- locking hooks (used by Bank.transfer) ----------

    public void lock() { lock.lock(); }
    public void unlock() { lock.unlock(); }

    // ---------- read access ----------

    public BigDecimal getBalance() {
        lock.lock();
        try {
            return balance;
        } finally {
            lock.unlock();
        }
    }

    /** Returns a read-only snapshot, so callers cannot modify our history. */
    public List<Transaction> getTransactions() {
        lock.lock();
        try {
            return Collections.unmodifiableList(new ArrayList<>(transactions));
        } finally {
            lock.unlock();
        }
    }

    public String getAccountNumber() { return accountNumber; }
    public String getHolderName() { return holderName; }

    public void printStatement() {
        System.out.println("--- Statement: " + accountNumber + " (" + holderName + ", " + getAccountType() + ") ---");
        List<Transaction> snapshot = getTransactions();
        if (snapshot.isEmpty()) {
            System.out.println("No transactions yet.");
        } else {
            snapshot.forEach(System.out::println);
        }
        System.out.println("Current balance: " + getBalance());
    }

    // ---------- helpers ----------

    /** Rounds to 2 decimals and rejects zero/negative values. */
    private static BigDecimal validate(BigDecimal amount) throws InvalidAmountException {
        if (amount == null) {
            throw new InvalidAmountException("Amount must not be null");
        }
        BigDecimal rounded = amount.setScale(2, RoundingMode.HALF_UP);
        if (rounded.signum() <= 0) {
            throw new InvalidAmountException("Amount must be greater than zero, got: " + amount);
        }
        return rounded;
    }

    @Override
    public String toString() {
        return accountNumber + " | " + holderName + " | " + getAccountType() + " | " + getBalance();
    }
}
