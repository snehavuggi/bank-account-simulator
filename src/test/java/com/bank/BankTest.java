package com.bank;

import com.bank.exception.AccountNotFoundException;
import com.bank.exception.InsufficientFundsException;
import com.bank.exception.InvalidAmountException;
import com.bank.model.Account;
import com.bank.model.Transaction;
import com.bank.model.TransactionType;
import com.bank.service.Bank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Tests for transfers between accounts. */
class BankTest {

    private Bank bank;
    private Account alice; // savings, 2000 (min 500)
    private Account bob;   // current, 100  (overdraft 1000)

    private static BigDecimal bd(String value) {
        return new BigDecimal(value);
    }

    @BeforeEach
    void setUp() throws Exception {
        bank = new Bank();
        alice = bank.createSavingsAccount("Alice", bd("2000"));
        bob = bank.createCurrentAccount("Bob", bd("100"));
    }

    @Test
    void transferMovesMoneyBetweenAccounts() throws Exception {
        bank.transfer(alice.getAccountNumber(), bob.getAccountNumber(), bd("300"));
        assertEquals(bd("1700.00"), alice.getBalance());
        assertEquals(bd("400.00"), bob.getBalance());
    }

    @Test
    void transferIsRecordedOnBothAccounts() throws Exception {
        bank.transfer(alice.getAccountNumber(), bob.getAccountNumber(), bd("300"));

        List<Transaction> aliceHistory = alice.getTransactions();
        List<Transaction> bobHistory = bob.getTransactions();
        assertEquals(TransactionType.TRANSFER_OUT, aliceHistory.get(aliceHistory.size() - 1).getType());
        assertEquals(TransactionType.TRANSFER_IN, bobHistory.get(bobHistory.size() - 1).getType());
    }

    @Test
    void transferTotalMoneyIsConserved() throws Exception {
        BigDecimal before = alice.getBalance().add(bob.getBalance());
        bank.transfer(alice.getAccountNumber(), bob.getAccountNumber(), bd("123.45"));
        assertEquals(before, alice.getBalance().add(bob.getBalance()));
    }

    @Test
    void failedTransferLeavesBothAccountsUntouched() {
        // Alice has 2000 but must keep 500, so max transferable is 1500
        assertThrows(InsufficientFundsException.class,
                () -> bank.transfer(alice.getAccountNumber(), bob.getAccountNumber(), bd("1500.01")));

        assertEquals(bd("2000.00"), alice.getBalance());
        assertEquals(bd("100.00"), bob.getBalance());
        assertEquals(1, alice.getTransactions().size()); // only the initial deposit
        assertEquals(1, bob.getTransactions().size());
    }

    @Test
    void transferUsesOverdraftOfSender() throws Exception {
        bank.transfer(bob.getAccountNumber(), alice.getAccountNumber(), bd("1100")); // 100 -> -1000
        assertEquals(bd("-1000.00"), bob.getBalance());
        assertEquals(bd("3100.00"), alice.getBalance());
    }

    @Test
    void transferToUnknownAccountFails() {
        assertThrows(AccountNotFoundException.class,
                () -> bank.transfer(alice.getAccountNumber(), "ACC9999999", bd("10")));
        assertEquals(bd("2000.00"), alice.getBalance());
    }

    @Test
    void transferFromUnknownAccountFails() {
        assertThrows(AccountNotFoundException.class,
                () -> bank.transfer("NOPE", bob.getAccountNumber(), bd("10")));
    }

    @Test
    void transferToSameAccountIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> bank.transfer(alice.getAccountNumber(), alice.getAccountNumber(), bd("10")));
    }

    @Test
    void transferOfInvalidAmountIsRejected() {
        assertThrows(InvalidAmountException.class,
                () -> bank.transfer(alice.getAccountNumber(), bob.getAccountNumber(), bd("-10")));
        assertThrows(InvalidAmountException.class,
                () -> bank.transfer(alice.getAccountNumber(), bob.getAccountNumber(), BigDecimal.ZERO));
        assertEquals(bd("2000.00"), alice.getBalance());
        assertEquals(bd("100.00"), bob.getBalance());
    }

    @Test
    void accountNumbersAreUnique() throws Exception {
        assertNotEquals(alice.getAccountNumber(), bob.getAccountNumber());
        assertSame(alice, bank.getAccount(alice.getAccountNumber()));
    }
}
