package com.bank.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;

/**
 * An immutable record of one money movement. Every field is final and there are
 * no setters, so a transaction can never be altered after it is created.
 */
public final class Transaction {

    private static final AtomicLong ID_GENERATOR = new AtomicLong(1);
    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final long id;
    private final TransactionType type;
    private final BigDecimal amount;
    private final BigDecimal balanceAfter;
    private final String description;
    private final LocalDateTime timestamp;

    public Transaction(TransactionType type, BigDecimal amount, BigDecimal balanceAfter, String description) {
        this.id = ID_GENERATOR.getAndIncrement(); // thread-safe unique ID
        this.type = type;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.description = description;
        this.timestamp = LocalDateTime.now();
    }

    public long getId() { return id; }
    public TransactionType getType() { return type; }
    public BigDecimal getAmount() { return amount; }
    public BigDecimal getBalanceAfter() { return balanceAfter; }
    public String getDescription() { return description; }
    public LocalDateTime getTimestamp() { return timestamp; }

    @Override
    public String toString() {
        return String.format("#%-4d %s  %-13s %12s  balance: %12s  %s",
                id, timestamp.format(FORMAT), type, amount, balanceAfter, description);
    }
}
