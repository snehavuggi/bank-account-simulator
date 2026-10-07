# Bank Account Simulator (Java)

A console banking simulator that demonstrates **OOP, exception handling, encapsulation, JUnit testing and thread-safety** in core Java.

## Features
- Savings accounts (minimum balance, monthly interest) and current accounts (overdraft limit)
- Deposits, withdrawals, transfers and a full transaction history / statement
- Money handled with `BigDecimal` (never `double`)
- Custom checked exceptions: `InsufficientFundsException`, `InvalidAmountException`, `AccountNotFoundException`
- **Thread-safe** using per-account `ReentrantLock`, with **deadlock-free transfers** (lock ordering)
- JUnit 5 tests for withdrawals, transfers and concurrent access
- A concurrency simulation that hammers the bank from 8 threads

## Project structure
```
src/main/java/com/bank/
 ├── Main.java                   console menu
 ├── ConcurrencySimulation.java  multi-threaded transfer demo
 ├── model/      Account, SavingsAccount, CurrentAccount, Transaction, TransactionType
 ├── service/    Bank
 └── exception/  InsufficientFundsException, InvalidAmountException, AccountNotFoundException
src/test/java/com/bank/
 ├── AccountTest.java            deposits / withdrawals
 ├── BankTest.java               transfers
 └── BankConcurrencyTest.java    thread-safety
```

## Requirements
- JDK 17+
- Maven 3.8+

## Build, test, run
```bash
mvn test                                   # run all JUnit tests
mvn compile
java -cp target/classes com.bank.Main                    # console app
java -cp target/classes com.bank.ConcurrencySimulation   # concurrency demo
```

## Design notes
- **Encapsulation:** balance and history are private; changes go only through validated `credit()`/`debit()`.
- **Atomic check-then-act:** the balance check and update in `debit()` happen under one lock.
- **Atomic transfers:** both accounts are locked, debit happens first, so a failure leaves both untouched.
- **Deadlock avoidance:** locks are always acquired in account-number order.
