package io.emax.cosigner.client.cli;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

import io.emax.cosigner.api.core.CurrencyParameters;
import io.emax.cosigner.api.core.CurrencyParametersRecipient;
import io.emax.cosigner.client.currency.CurrencyConnector;
import io.emax.cosigner.client.currency.MonitorWebSocket;

import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.Collections;


/**
 * Command line option for running the library.
 *
 * @author Tom
 */
public class Application {
  /**
   * Command line interface that provides basic access to the library.
   *
   * @param args Command line arguments, leave blank to see usage.
   */
  public static void main(String[] args) throws Exception {
    Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
    root.setLevel(Level.INFO);

    if (args.length < 1) {
      System.out.println("Usage: <interfaceMethod> <argument> <argument> ...");
      System.out.println("Available methods:");
      System.out.println("\tlistCurrencies()");
      System.out.println("\tregisterAddress(String currency, String address, String accountName)");
      System.out.println("\tgetNewAddress(String currency, String accountName)");
      System.out.println("\tlistAllAddresses(String currency, String accountName)");
      System.out.println("\tlistTransactions(String currency, String address)");
      System.out.println("\tgetBalance(String currency, String address)");
      System.out.println("\tprepareTransaction(String currency, String fromAddress,"
          + "String toAddress, Decimal amount)");
      System.out.println("\tgetSigners(String currency, String transaction)");
      System.out
          .println("\tapproveTransaction(String currency, String transaction, String address)");
      System.out.println("\tbroadcastTransaction(String currency, String transaction)");
      System.out.println("\tmonitorAddress(String currency, String address)");
      return;
    }

    CurrencyConnector connector = new CurrencyConnector();
    CurrencyParameters params = new CurrencyParameters();
    String accountName = "";
    String currency = "";
    String address = "";
    String rcptAddress = "";
    BigDecimal amount = BigDecimal.ZERO;
    CurrencyParametersRecipient rcpt = new CurrencyParametersRecipient();
    String tx = "";
    switch (args[0]) {
      case "listCurrencies":
        System.out.println(connector.listCurrencies());
        break;
      case "registerAddress":
        if (args.length >= 2) {
          currency = args[1];
        }
        if (args.length >= 3) {
          address = args[2];
        }
        if (args.length >= 4) {
          accountName = args[3];
        }
        params.setCurrencySymbol(currency);
        params.setUserKey(accountName);
        params.setAccount(Collections.singletonList(address));
        System.out.println(connector.registerAddress(params));
        break;
      case "getNewAddress":
        if (args.length >= 2) {
          currency = args[1];
        }
        if (args.length >= 3) {
          accountName = args[2];
        }
        params.setCurrencySymbol(currency);
        params.setUserKey(accountName);
        System.out.println(connector.getNewAddress(params));
        break;
      case "listAllAddresses":
        if (args.length >= 2) {
          currency = args[1];
        }
        if (args.length >= 3) {
          accountName = args[2];
        }
        params.setCurrencySymbol(currency);
        params.setUserKey(accountName);
        System.out.println(connector.listAllAddresses(params));
        break;
      case "listTransactions":
        if (args.length >= 2) {
          currency = args[1];
        }
        if (args.length >= 3) {
          address = args[2];
        }
        params.setCurrencySymbol(currency);
        params.setAccount(Collections.singletonList(address));
        System.out.println(connector.listTransactions(params));
        break;
      case "getBalance":
        if (args.length >= 2) {
          currency = args[1];
        }
        if (args.length >= 3) {
          address = args[2];
        }
        params.setCurrencySymbol(currency);
        params.setAccount(Collections.singletonList(address));
        System.out.println(connector.getBalance(params));
        break;
      case "prepareTransaction":
        if (args.length >= 2) {
          currency = args[1];
        }
        if (args.length >= 3) {
          address = args[2];
        }
        if (args.length >= 4) {
          rcptAddress = args[3];
        }
        if (args.length >= 5) {
          amount = new BigDecimal(args[4]);
        }
        if (args.length >= 6) {
          accountName = args[5];
        }
        params.setCurrencySymbol(currency);
        params.setUserKey(accountName);
        params.setAccount(Collections.singletonList(address));
        rcpt.setAmount(amount.toPlainString());
        rcpt.setRecipientAddress(rcptAddress);
        params.setReceivingAccount(Collections.singletonList(rcpt));
        System.out.println(connector.prepareTransaction(params));
        break;
      case "getSigners":
        if (args.length >= 2) {
          currency = args[1];
        }
        if (args.length >= 3) {
          tx = args[2];
        }
        params.setCurrencySymbol(currency);
        params.setTransactionData(tx);
        System.out.println(connector.getSignersForTransaction(params));
        break;
      case "approveTransaction":
        if (args.length >= 2) {
          currency = args[1];
        }
        if (args.length >= 3) {
          tx = args[2];
        }
        if (args.length >= 4) {
          address = args[3];
        }
        params.setCurrencySymbol(currency);
        params.setAccount(Collections.singletonList(address));
        params.setTransactionData(tx);
        System.out.println(connector.approveTransaction(params));
        break;
      case "broadcastTransaction":
        if (args.length >= 2) {
          currency = args[1];
        }
        if (args.length >= 3) {
          tx = args[2];
        }
        params.setCurrencySymbol(currency);
        params.setTransactionData(tx);
        System.out.println(connector.broadcastTransaction(params));
        break;
      default:
        System.out.println("Method not valid or not supported yet");
        break;
      case "monitorAddress":
        if (args.length >= 2) {
          currency = args[1];
        }
        if (args.length >= 3) {
          address = args[2];
        }
        params.setCurrencySymbol(currency);
        params.setAccount(Collections.singletonList(address));

        System.out.println("Hit Ctrl+C to close the websocket.");
        MonitorWebSocket socket = connector.monitorBalance(params);
        while (true) {
          // Sleep 5 seconds
          Thread.sleep(1000 * 5);

          // Print the information
          socket.getAllBalances().forEach((balAddress, balance) -> System.out
              .println("Address: " + balAddress + " Balance: " + balance));
          socket.getNewTransactions()
              .forEach(transaction -> System.out.println("New Transaction: " + transaction));
        }
    }
  }
}
