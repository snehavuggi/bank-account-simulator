package com.bank;

import com.bank.exception.AccountNotFoundException;
import com.bank.exception.InsufficientFundsException;
import com.bank.exception.InvalidAmountException;
import com.bank.model.Account;
import com.bank.service.Bank;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Fires thousands of transfers from many threads at the same two accounts,
 * in BOTH directions at once (the classic deadlock scenario), then checks that
 * no money was created or destroyed.
 *
 * Run: java -cp target/classes com.bank.ConcurrencySimulation
 */
public class ConcurrencySimulation {

    public static void main(String[] args) throws Exception {
        final int threads = 8;
        final int transfersPerThread = 5_000;

        Bank bank = new Bank();
        Account alice = bank.createSavingsAccount("Alice", new BigDecimal("10000"));
        Account bob = bank.createCurrentAccount("Bob", new BigDecimal("10000"));

        BigDecimal totalBefore = alice.getBalance().add(bob.getBalance());
        System.out.println("Before: Alice=" + alice.getBalance() + "  Bob=" + bob.getBalance()
                + "  Total=" + totalBefore);

        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch startGun = new CountDownLatch(1); // all threads begin together
        AtomicInteger succeeded = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();

        for (int t = 0; t < threads; t++) {
            final boolean aliceToBob = (t % 2 == 0); // half the threads go each way
            pool.submit(() -> {
                try {
                    startGun.await();
                    for (int i = 0; i < transfersPerThread; i++) {
                        BigDecimal amount = BigDecimal.valueOf(ThreadLocalRandom.current().nextInt(1, 51));
                        String from = aliceToBob ? alice.getAccountNumber() : bob.getAccountNumber();
                        String to = aliceToBob ? bob.getAccountNumber() : alice.getAccountNumber();
                        try {
                            bank.transfer(from, to, amount);
                            succeeded.incrementAndGet();
                        } catch (InsufficientFundsException e) {
                            rejected.incrementAndGet(); // legitimate business rejection
                        } catch (AccountNotFoundException | InvalidAmountException e) {
                            throw new IllegalStateException(e);
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        long start = System.nanoTime();
        startGun.countDown();
        pool.shutdown();
        if (!pool.awaitTermination(60, TimeUnit.SECONDS)) {
            System.out.println("DEADLOCK? Threads did not finish within 60s");
            pool.shutdownNow();
            return;
        }
        long millis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);

        BigDecimal totalAfter = alice.getBalance().add(bob.getBalance());
        System.out.println("After:  Alice=" + alice.getBalance() + "  Bob=" + bob.getBalance()
                + "  Total=" + totalAfter);
        System.out.println("Transfers succeeded: " + succeeded + ", rejected (insufficient funds): " + rejected
                + ", time: " + millis + " ms");
        System.out.println(totalBefore.compareTo(totalAfter) == 0
                ? "PASS: total money unchanged, no deadlock."
                : "FAIL: money was created or lost!");
    }
}
