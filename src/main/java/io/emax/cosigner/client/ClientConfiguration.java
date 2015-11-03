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
  private static String rsServerUrl = "https://localhost:8443";
  private static String wsServerUrl = "wss://localhost:8443";
  private static boolean useTls = true;
  private static String tlsKeystore = "./cosigner.jks";
  private static String tlsKeystorePassword = "cosigner";
  private static String tlsCertAlias = "cosigner";

  public String getRsServerUrl() {
    return rsServerUrl;
  }

  public String getWsServerUrl() {
    return wsServerUrl;
  }

  public boolean useTls() {
    return useTls;
  }

  public String getTlsKeystore() {
    return tlsKeystore;
  }

  public String getTlsKeystorePassword() {
    return tlsKeystorePassword;
  }

  public String getTlsCertAlias() {
    return tlsCertAlias;
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
        // rsServerUrl
        rsServerUrl = cosignerProperties.getProperty("rsServerUrl", rsServerUrl);

        // wsServerUrl
        wsServerUrl = cosignerProperties.getProperty("wsServerUrl", wsServerUrl);

        // tlsKeystore
        tlsKeystore = cosignerProperties.getProperty("tlsKeystore", tlsKeystore);

        // tlsKeystorePassword
        tlsKeystorePassword =
            cosignerProperties.getProperty("tlsKeystorePassword", tlsKeystorePassword);

        // tlsCertAlias
        tlsCertAlias = cosignerProperties.getProperty("tlsCertAlias", tlsCertAlias);

        // useTls
        useTls =
            Boolean.parseBoolean(cosignerProperties.getProperty("useTls", String.valueOf(useTls)));

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

