package com.bank;

import com.bank.exception.InsufficientFundsException;
import com.bank.model.Account;
import com.bank.service.Bank;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/** Proves the bank is thread-safe and deadlock-free under concurrent load. */
class BankConcurrencyTest {

    private static BigDecimal bd(String value) {
        return new BigDecimal(value);
    }

    /** Runs the given tasks on a thread pool, all starting at the same instant. */
    private static void runConcurrently(int threads, List<Callable<Void>> tasks) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch startGun = new CountDownLatch(1);
        List<Future<Void>> futures = new ArrayList<>();
        for (Callable<Void> task : tasks) {
            futures.add(pool.submit(() -> {
                startGun.await();
                return task.call();
            }));
        }
        startGun.countDown();
        for (Future<Void> f : futures) {
            f.get(30, TimeUnit.SECONDS); // rethrows any exception from the task
        }
        pool.shutdownNow();
    }

    @Test
    @Timeout(60)
    void concurrentDepositsDoNotLoseUpdates() throws Exception {
        Bank bank = new Bank();
        Account account = bank.createCurrentAccount("Alice", BigDecimal.ZERO);

        int threads = 8;
        int depositsPerThread = 1_000;
        List<Callable<Void>> tasks = new ArrayList<>();
        for (int t = 0; t < threads; t++) {
            tasks.add(() -> {
                for (int i = 0; i < depositsPerThread; i++) {
                    account.deposit(bd("1"));
                }
                return null;
            });
        }
        runConcurrently(threads, tasks);

        assertEquals(bd("8000.00"), account.getBalance());
        assertEquals(8_000, account.getTransactions().size());
    }

    @Test
    @Timeout(60)
    void oppositeDirectionTransfersDoNotDeadlockAndConserveMoney() throws Exception {
        Bank bank = new Bank();
        Account a = bank.createSavingsAccount("Alice", bd("10000"));
        Account b = bank.createCurrentAccount("Bob", bd("10000"));
        BigDecimal totalBefore = a.getBalance().add(b.getBalance());

        int threads = 8;
        int transfersPerThread = 2_000;
        AtomicInteger succeeded = new AtomicInteger();
        List<Callable<Void>> tasks = new ArrayList<>();
        for (int t = 0; t < threads; t++) {
            boolean aToB = (t % 2 == 0);
            tasks.add(() -> {
                for (int i = 0; i < transfersPerThread; i++) {
                    try {
                        if (aToB) {
                            bank.transfer(a.getAccountNumber(), b.getAccountNumber(), bd("7"));
                        } else {
                            bank.transfer(b.getAccountNumber(), a.getAccountNumber(), bd("7"));
                        }
                        succeeded.incrementAndGet();
                    } catch (InsufficientFundsException ignored) {
                        // allowed: business rule rejected this one
                    }
                }
                return null;
            });
        }
        runConcurrently(threads, tasks);

        assertEquals(totalBefore, a.getBalance().add(b.getBalance()));
        assertTrue(succeeded.get() > 0);
        // balances must still respect each account's rules
        assertTrue(a.getBalance().compareTo(bd("500")) >= 0);
        assertTrue(b.getBalance().compareTo(bd("-1000")) >= 0);
    }

    @Test
    @Timeout(60)
    void concurrentWithdrawalsNeverOverdrawBeyondLimit() throws Exception {
        Bank bank = new Bank();
        // Savings: 1500 with 500 minimum -> exactly 1000 withdrawable in total
        Account savings = bank.createSavingsAccount("Alice", bd("1500"));

        int threads = 10;
        AtomicInteger succeeded = new AtomicInteger();
        List<Callable<Void>> tasks = new ArrayList<>();
        for (int t = 0; t < threads; t++) {
            tasks.add(() -> {
                for (int i = 0; i < 100; i++) { // 1000 attempts of 5 = 5000 requested
                    try {
                        savings.withdraw(bd("5"));
                        succeeded.incrementAndGet();
                    } catch (InsufficientFundsException ignored) {
                        // expected once the limit is reached
                    }
                }
                return null;
            });
        }
        runConcurrently(threads, tasks);

        assertEquals(200, succeeded.get()); // 200 x 5 = 1000 exactly
        assertEquals(bd("500.00"), savings.getBalance());
    }
}
