package io.emax.cosigner.client.currency;

import io.emax.cosigner.api.core.CurrencyPackage;
import io.emax.cosigner.api.core.CurrencyParameters;
import io.emax.cosigner.api.currency.Wallet;
import io.emax.cosigner.bitcoin.BitcoinConfiguration;
import io.emax.cosigner.bitcoin.BitcoinWallet;
import io.emax.cosigner.client.ClientConfiguration;
import io.emax.cosigner.common.Json;
import io.emax.cosigner.ethereum.EthereumConfiguration;
import io.emax.cosigner.ethereum.EthereumWallet;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.LinkedList;
import java.util.concurrent.Future;


public class CurrencyConnector {
  private static final Logger logger = LoggerFactory.getLogger(CurrencyConnector.class);
  private final ClientConfiguration config = new ClientConfiguration();
  private HttpClient httpClient = new HttpClient();
  private WebSocketClient webSocketClient = new WebSocketClient();
  private final MonitorWebSocket monitorSocket = new MonitorWebSocket();

  // TODO create a local signing method for "bring-your-own-keys" setups.

  /**
   * Constructor for the connector.
   *
   * <p>Sets up TLS if it's configured.
   */
  public CurrencyConnector() {
    if (config.useTls()) {
      SslContextFactory sslContext = new SslContextFactory(config.getTlsKeystore());
      sslContext.setKeyStorePassword(config.getTlsKeystorePassword());
      sslContext.setTrustStorePath(config.getTlsKeystore());
      sslContext.setTrustStorePassword(config.getTlsKeystorePassword());
      sslContext.setCertAlias(config.getTlsCertAlias());
      httpClient = new HttpClient(sslContext);
      webSocketClient = new WebSocketClient(sslContext);
    }
  }

  private String restPostRequest(String endpoint, String content) {
    try {
      logger.debug("Sending POST request to: " + config.getRsServerUrl() + endpoint);
      httpClient.start();
      Request request = httpClient.newRequest(config.getRsServerUrl() + endpoint);
      request = request.method(HttpMethod.POST);
      request = request.content(new StringContentProvider(content, "UTF-8"));
      ContentResponse response = request.send();
      logger.debug("Got response: " + response.getContentAsString());
      return response.getContentAsString();
    } catch (Exception e) {
      logger.error(null, e);
      return "";
    } finally {
      try {
        httpClient.stop();
      } catch (Exception e) {
        logger.error(null, e);
      }
    }
  }

  private String restGetRequest(String endpoint) {
    try {
      logger.debug("Sending GET request to: " + config.getRsServerUrl() + endpoint);
      httpClient.start();
      Request request = httpClient.newRequest(config.getRsServerUrl() + endpoint);
      request = request.method(HttpMethod.GET);
      ContentResponse response = request.send();
      logger.debug("Got response: " + response.getContentAsString());
      return response.getContentAsString();
    } catch (Exception e) {
      logger.error(null, e);
      return "";
    } finally {
      try {
        httpClient.stop();
      } catch (Exception e) {
        logger.error(null, e);
      }
    }
  }

  /**
   * Get wallets, with optional filter on currency code.
   */
  @SuppressWarnings("unchecked")
  public Iterable<Wallet> getWallets(Iterable<String> currencies, boolean onlyShowSupported) {
    // Build collection of all known currencies first.
    LinkedList<CurrencyPackage> currencyPackages = new LinkedList<>();
    // BTC
    CurrencyPackage bitcoinPackage = new CurrencyPackage();
    bitcoinPackage.setConfiguration(new BitcoinConfiguration());
    bitcoinPackage.setWallet(new BitcoinWallet());
    currencyPackages.add(bitcoinPackage);
    // ETH
    CurrencyPackage ethereumPackage = new CurrencyPackage();
    ethereumPackage.setConfiguration(new EthereumConfiguration());
    ethereumPackage.setWallet(new EthereumWallet());
    currencyPackages.add(ethereumPackage);

    LinkedList<String> supportedCurrencies = new LinkedList<>();
    if (onlyShowSupported) {
      supportedCurrencies
          .addAll((LinkedList<String>) Json.objectifyString(LinkedList.class, listCurrencies()));
    }

    LinkedList<Wallet> wallets = new LinkedList<>();
    currencyPackages.forEach(currency -> {
      if (onlyShowSupported && supportedCurrencies
          .contains(currency.getConfiguration().getCurrencySymbol()) || !onlyShowSupported) {
        if (currencies != null && currencies.iterator().hasNext()) {
          currencies.forEach(filter -> {
            if (filter.equalsIgnoreCase(currency.getConfiguration().getCurrencySymbol())) {
              wallets.add(currency.getWallet());
            }
          });
        } else {
          wallets.add(currency.getWallet());
        }
      }
    });

    return wallets;
  }

  /**
   * List currencies provided by cosigner server.
   */
  public String listCurrencies() {
    return restGetRequest("/rs/ListCurrencies");
  }

  /**
   * Registers addresses for currency libraries that need a watch list.
   */
  public String registerAddress(CurrencyParameters params) {
    String paramString = Json.stringifyObject(CurrencyParameters.class, params);
    return restPostRequest("/rs/RegisterAddress", paramString);
  }

  /**
   * Get a new address.
   */
  public String getNewAddress(CurrencyParameters params) {
    String paramString = Json.stringifyObject(CurrencyParameters.class, params);
    return restPostRequest("/rs/GetNewAddress", paramString);
  }

  /**
   * Convert a public key into the relevant address.
   */
  public String convertKeytoAddress(CurrencyParameters params) {
    String paramString = Json.stringifyObject(CurrencyParameters.class, params);
    return restPostRequest("/rs/GenerateAddressFromKey", paramString);
  }

  /**
   * List all addresses that we have generated for the given user key and currency.
   */
  public String listAllAddresses(CurrencyParameters params) {
    String paramString = Json.stringifyObject(CurrencyParameters.class, params);
    return restPostRequest("/rs/ListAllAddresses", paramString);
  }

  /**
   * List transactions for the given address and currency.
   */
  public String listTransactions(CurrencyParameters params) {
    String paramString = Json.stringifyObject(CurrencyParameters.class, params);
    return restPostRequest("/rs/ListTransactions", paramString);
  }

  /**
   * Returns the combined balance of all addresses provided in the parameters.
   */
  public String getBalance(CurrencyParameters params) {
    String paramString = Json.stringifyObject(CurrencyParameters.class, params);
    return restPostRequest("/rs/GetBalance", paramString);
  }

  /**
   * Sets up a monitor for the given addresses.
   *
   * <p>A monitor provides periodic balance updates, along with all known transactions when
   * initialized, and any new transactions that come in while it's active. Transactions can be
   * distinguished from balance updates in that the transaction data portion of the response has
   * data, it contains the transaction hash.
   */
  public MonitorWebSocket monitorBalance(CurrencyParameters params) {
    try {
      logger.debug("Connecting to websocket: " + config.getWsServerUrl() + "/ws/MonitorBalance");
      logger.debug(
          "Starting websocket with: " + Json.stringifyObject(CurrencyParameters.class, params));

      if (!webSocketClient.isStarted()) {
        webSocketClient.start();
      }
      Future<Session> session = webSocketClient
          .connect(monitorSocket, new URI(config.getWsServerUrl() + "/ws/MonitorBalance"));

      session.get().getRemote().sendString(Json.stringifyObject(CurrencyParameters.class, params));

      return monitorSocket;
    } catch (Exception e) {
      logger.error(null, e);
      try {
        webSocketClient.stop();
      } catch (Exception e1) {
        logger.error(null, e);
      }
      return null;
    }
  }

  /**
   * Create and sign a transaction.
   *
   * <p>This only signs the transaction with the user's key, showing that the user has requested the
   * transaction. The server keys are not used until the approve stage.
   */
  public String prepareTransaction(CurrencyParameters params) {
    String paramString = Json.stringifyObject(CurrencyParameters.class, params);
    return restPostRequest("/rs/PrepareTransaction", paramString);
  }

  /**
   * Get list of addresses that could sign the transaction.
   */
  @SuppressWarnings("unchecked")
  public Iterable<String> getSignersForTransaction(CurrencyParameters params) {
    String paramString = Json.stringifyObject(CurrencyParameters.class, params);
    String response = restPostRequest("/rs/GetSignersForTransaction", paramString);
    return (Iterable<String>) Json.objectifyString(Iterable.class, response);
  }

  /**
   * Get signing data for offline signature.
   */
  @SuppressWarnings("unchecked")
  public Iterable<Iterable<String>> getSignatureString(CurrencyParameters params) {
    String paramString = Json.stringifyObject(CurrencyParameters.class, params);
    String response = restPostRequest("/rs/GetSignatureString", paramString);
    return (Iterable<Iterable<String>>) Json.objectifyString(Iterable.class, response);
  }

  /**
   * Apply an offline signature to transaction.
   */
  public String applySignature(CurrencyParameters params) {
    String paramString = Json.stringifyObject(CurrencyParameters.class, params);
    return restPostRequest("/rs/ApplySignature", paramString);
  }

  /**
   * Approve a transaction that's been signed off on by the user.
   *
   * <p>This stage signs the transaction with the server keys after running it through any sanity
   * checks and validation required.
   */
  public String approveTransaction(CurrencyParameters params) {
    String paramString = Json.stringifyObject(CurrencyParameters.class, params);
    return restPostRequest("/rs/ApproveTransaction", paramString);
  }

  /**
   * Submits a transaction for processing on the network.
   */
  public String submitTransaction(CurrencyParameters params) {
    String paramString = Json.stringifyObject(CurrencyParameters.class, params);
    return restPostRequest("/rs/SubmitTransaction", paramString);
  }
}
