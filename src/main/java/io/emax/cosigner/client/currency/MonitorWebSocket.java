package io.emax.cosigner.client.currency;

import io.emax.cosigner.api.core.CurrencyParameters;
import io.emax.cosigner.common.Json;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;

@WebSocket(maxIdleTime = 1200000)
public class MonitorWebSocket {
  private static final Logger logger = LoggerFactory.getLogger(MonitorWebSocket.class);
  private Session session;

  private String buffer = "";

  private final HashMap<String, String> balances = new HashMap<>();
  private final HashSet<CurrencyParameters> allTransactions = new HashSet<>();
  private final HashSet<CurrencyParameters> newTransactions = new HashSet<>();

  public HashMap<String, String> getAllBalances() {
    return balances;
  }

  public HashSet<CurrencyParameters> getAllTransactions() {
    return allTransactions;
  }

  /**
   * Returns the new transactions since the last time this was called.
   */
  public HashSet<CurrencyParameters> getNewTransactions() {
    HashSet<CurrencyParameters> retSet = new HashSet<>();
    retSet.addAll(newTransactions);
    newTransactions.clear();
    return retSet;
  }

  @OnWebSocketClose
  public void onClose(int statusCode, String reason) {
    logger.info("Connection closed: " + statusCode + " - " + reason);
    this.session = null;
  }

  @OnWebSocketConnect
  public void onConnect(Session session) {
    logger.debug("Got connect: " + session);
    this.session = session;
  }

  /**
   * We got a message.
   */
  @OnWebSocketMessage
  public void onMessage(String msg) {
    logger.debug("Got message: " + msg);

    buffer += msg;

    while (!buffer.isEmpty()) {
      // Read past whitespace
      while (buffer.startsWith(" ")) {
        buffer = buffer.substring(1);
      }

      // Read the size
      int sizePos = buffer.indexOf("|");
      if (sizePos == -1) {
        logger.debug("Incomplete message");
        return;
      }
      int size = Integer.parseInt(buffer.substring(0, sizePos));

      // Read the message if it's all there.
      if (buffer.substring(sizePos + 1).length() < size) {
        logger.debug("Got a size, message too short.");
        return;
      }
      String message = buffer.substring(sizePos + 1, sizePos + 1 + size);

      // Advance the buffer.
      buffer = buffer.substring(sizePos + 1 + size);

      // Parse the incoming message into a CurrencyParameters object
      CurrencyParameters params =
          (CurrencyParameters) Json.objectifyString(CurrencyParameters.class, message);

      if (params == null) {
        logger.warn("Got bad websocket message: " + message);
        continue;
      }

      if (params.getTransactionData() == null || params.getTransactionData().isEmpty()) {
        if (params.getReceivingAccount() == null) {
          continue;
        }
        params.getReceivingAccount()
            .forEach(account -> balances.put(account.getRecipientAddress(), account.getAmount()));
      } else {
        // TX update
        if (!allTransactions.contains(params)) {
          newTransactions.add(params);
          allTransactions.add(params);
        }
      }
    }
  }

  public void closeConnection() {
    session.close();
  }
}
