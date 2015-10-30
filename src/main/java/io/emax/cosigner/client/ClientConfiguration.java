package io.emax.cosigner.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Properties;

public class ClientConfiguration {
  private static final Logger logger = LoggerFactory.getLogger(ClientConfiguration.class);
  private static boolean configLoaded = false;

  // Configuration data
  private static String serverUrl = "http://localhost:8080";
  private static String caCert = "ca.pem";
  private static String clientCert = "client.pem";
  private static String clientKey = "client.key";

  public String getServerUrl() {
    return serverUrl;
  }

  public String getCaCert() {
    return caCert;
  }

  public String getClientCert() {
    return clientCert;
  }

  public String getClientKey() {
    return clientKey;
  }

  private static synchronized void loadConfig() {
    if (!configLoaded) {
      FileInputStream propertiesFile = null;
      try {
        // Get the config file
        String propertiesFilePath = "./cosigner-client.properties";
        Properties cosignerProperties = new Properties();
        propertiesFile = new FileInputStream(propertiesFilePath);
        cosignerProperties.load(propertiesFile);
        propertiesFile.close();

        // Load config
        // serverUrl
        serverUrl = cosignerProperties.getProperty("serverUrl", serverUrl);
        
        // caCert
        caCert = cosignerProperties.getProperty("caCert", caCert);
        
        // clientCert
        clientCert = cosignerProperties.getProperty("clientCert", clientCert);
        
        // clientKey 
        clientKey = cosignerProperties.getProperty("clientKey", clientKey);
        
      } catch (IOException e) {
        if (propertiesFile != null) {
          try {
            propertiesFile.close();
          } catch (IOException e1) {
            StringWriter errors = new StringWriter();
            e1.printStackTrace(new PrintWriter(errors));
            logger.warn(errors.toString());
          }
        }
        logger.info("Could not load cosigner-client configuration, using defaults.");
      }
      configLoaded = true;
    }
  }

  public ClientConfiguration() {
    loadConfig();
  }
}

