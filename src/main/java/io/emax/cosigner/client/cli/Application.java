package io.emax.cosigner.client.cli;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

import io.emax.cosigner.api.core.CurrencyParameters;
import io.emax.cosigner.client.currency.CurrencyConnector;

import org.slf4j.LoggerFactory;


/**
 * Command line option for running the library.
 * 
 * @author Tom
 *
 */
public class Application {
  /**
   * Command line interface that provides basic access to the library.
   * 
   * @param args Command line arguments, leave blank to see usage.
   */
  public static void main(String[] args) {
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
      System.out
          .println("\tprepareTransaction(String currency, String fromAddress1, String fromAddress2,"
              + " ..., String toAddress, Decimal amount)");
      System.out
          .println("\tapproveTransaction(String currency, String transaction, String address)");
      System.out.println("\tsendTransaction(String currency, String transaction)");
      return;
    }

    CurrencyConnector connector = new CurrencyConnector();
    CurrencyParameters params = new CurrencyParameters();
    String accountName = "";
    String currency = "";
    switch (args[0]) {
      case "listCurrencies":
        System.out.println(connector.listCurrencies());
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
      default:
        System.out.println("Method not valid or not supported yet");
    }
  }
}
