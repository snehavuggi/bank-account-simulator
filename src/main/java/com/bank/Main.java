package com.bank;

import com.bank.exception.AccountNotFoundException;
import com.bank.exception.InsufficientFundsException;
import com.bank.exception.InvalidAmountException;
import com.bank.model.Account;
import com.bank.model.SavingsAccount;
import com.bank.service.Bank;

import java.math.BigDecimal;
import java.util.Scanner;

/** Console menu that drives the Bank. */
public class Main {

    private static final Bank bank = new Bank();
    private static final Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        System.out.println("=== Bank Account Simulator ===");
        boolean running = true;
        while (running) {
            printMenu();
            String choice = scanner.nextLine().trim();
            try {
                switch (choice) {
                    case "1" -> createAccount(true);
                    case "2" -> createAccount(false);
                    case "3" -> deposit();
                    case "4" -> withdraw();
                    case "5" -> transfer();
                    case "6" -> bank.getAccount(ask("Account number: ")).printStatement();
                    case "7" -> System.out.println("Balance: " + bank.getAccount(ask("Account number: ")).getBalance());
                    case "8" -> bank.getAllAccounts().forEach(System.out::println);
                    case "9" -> applyInterest();
                    case "0" -> running = false;
                    default -> System.out.println("Invalid option, try again.");
                }
            } catch (AccountNotFoundException | InvalidAmountException | InsufficientFundsException e) {
                System.out.println("Error: " + e.getMessage());
            } catch (NumberFormatException e) {
                System.out.println("Error: please enter a valid number, e.g. 250.50");
            } catch (IllegalArgumentException e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
        System.out.println("Goodbye!");
    }

    private static void printMenu() {
        System.out.println("""

                1. Create savings account     6. Show statement
                2. Create current account     7. Check balance
                3. Deposit                    8. List all accounts
                4. Withdraw                   9. Apply monthly interest (savings)
                5. Transfer                   0. Exit""");
        System.out.print("Choose: ");
    }

    private static void createAccount(boolean savings) throws InvalidAmountException {
        String name = ask("Holder name: ");
        BigDecimal initial = askAmount("Initial deposit: ");
        Account account = savings
                ? bank.createSavingsAccount(name, initial)
                : bank.createCurrentAccount(name, initial);
        System.out.println("Created: " + account);
    }

    private static void deposit() throws AccountNotFoundException, InvalidAmountException {
        Account account = bank.getAccount(ask("Account number: "));
        account.deposit(askAmount("Amount: "));
        System.out.println("Deposit successful. New balance: " + account.getBalance());
    }

    private static void withdraw()
            throws AccountNotFoundException, InvalidAmountException, InsufficientFundsException {
        Account account = bank.getAccount(ask("Account number: "));
        account.withdraw(askAmount("Amount: "));
        System.out.println("Withdrawal successful. New balance: " + account.getBalance());
    }

    private static void transfer()
            throws AccountNotFoundException, InvalidAmountException, InsufficientFundsException {
        String from = ask("From account: ");
        String to = ask("To account: ");
        bank.transfer(from, to, askAmount("Amount: "));
        System.out.println("Transfer successful.");
    }

    private static void applyInterest() throws AccountNotFoundException, InvalidAmountException {
        Account account = bank.getAccount(ask("Account number: "));
        if (account instanceof SavingsAccount savings) {
            savings.applyMonthlyInterest();
            System.out.println("Interest applied. New balance: " + savings.getBalance());
        } else {
            System.out.println("Interest only applies to savings accounts.");
        }
    }

    private static String ask(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }

    private static BigDecimal askAmount(String prompt) {
        return new BigDecimal(ask(prompt)); // NumberFormatException handled in main loop
    }
}
