package com.bank;

import com.bank.exception.InsufficientFundsException;
import com.bank.exception.InvalidAmountException;
import com.bank.model.CurrentAccount;
import com.bank.model.SavingsAccount;
import com.bank.model.TransactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Tests for deposits and withdrawals on a single account. */
class AccountTest {

    private SavingsAccount savings;
    private CurrentAccount current;

    private static BigDecimal bd(String value) {
        return new BigDecimal(value);
    }

    @BeforeEach
    void setUp() throws Exception {
        savings = new SavingsAccount("S1", "Alice");
        savings.deposit(bd("1000"));
        current = new CurrentAccount("C1", "Bob");
        current.deposit(bd("100"));
    }

    @Test
    void depositIncreasesBalance() throws Exception {
        savings.deposit(bd("250.50"));
        assertEquals(bd("1250.50"), savings.getBalance());
    }

    @Test
    void withdrawalReducesBalance() throws Exception {
        savings.withdraw(bd("300"));
        assertEquals(bd("700.00"), savings.getBalance());
    }

    @Test
    void savingsCanWithdrawDownToExactMinimum() throws Exception {
        savings.withdraw(bd("500")); // 1000 - 500 = 500 = minimum, allowed
        assertEquals(bd("500.00"), savings.getBalance());
    }

    @Test
    void savingsCannotGoBelowMinimum() {
        assertThrows(InsufficientFundsException.class, () -> savings.withdraw(bd("500.01")));
    }

    @Test
    void currentAccountAllowsOverdraftUpToLimit() throws Exception {
        current.withdraw(bd("1100")); // 100 - 1100 = -1000 = overdraft limit
        assertEquals(bd("-1000.00"), current.getBalance());
    }

    @Test
    void currentAccountCannotExceedOverdraftLimit() {
        assertThrows(InsufficientFundsException.class, () -> current.withdraw(bd("1100.01")));
    }

    @Test
    void zeroNegativeAndNullAmountsAreRejected() {
        assertThrows(InvalidAmountException.class, () -> savings.withdraw(BigDecimal.ZERO));
        assertThrows(InvalidAmountException.class, () -> savings.withdraw(bd("-5")));
        assertThrows(InvalidAmountException.class, () -> savings.deposit(null));
        assertThrows(InvalidAmountException.class, () -> savings.deposit(bd("0.001"))); // rounds to 0.00
    }

    @Test
    void failedWithdrawalChangesNothing() {
        int historyBefore = savings.getTransactions().size();
        assertThrows(InsufficientFundsException.class, () -> savings.withdraw(bd("999999")));
        assertEquals(bd("1000.00"), savings.getBalance());
        assertEquals(historyBefore, savings.getTransactions().size());
    }

    @Test
    void successfulWithdrawalIsRecordedInHistory() throws Exception {
        savings.withdraw(bd("100"));
        List<com.bank.model.Transaction> history = savings.getTransactions();
        com.bank.model.Transaction last = history.get(history.size() - 1);
        assertEquals(TransactionType.WITHDRAWAL, last.getType());
        assertEquals(bd("100.00"), last.getAmount());
        assertEquals(bd("900.00"), last.getBalanceAfter());
    }

    @Test
    void transactionHistoryCannotBeModifiedFromOutside() {
        assertThrows(UnsupportedOperationException.class, () -> savings.getTransactions().clear());
    }

    @Test
    void monthlyInterestIsCredited() throws Exception {
        savings.applyMonthlyInterest(); // 1000 * 4% / 12 = 3.33
        assertEquals(bd("1003.33"), savings.getBalance());
    }
}
