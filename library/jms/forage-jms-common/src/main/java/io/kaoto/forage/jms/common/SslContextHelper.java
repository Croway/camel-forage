package io.kaoto.forage.jms.common;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SslContextHelper {

    private static final Logger LOG = LoggerFactory.getLogger(SslContextHelper.class);

    private SslContextHelper() {}

    public static SSLContext createSslContext(ConnectionFactoryConfig config) {
        if (!config.sslEnabled()) {
            return null;
        }

        try {
            TrustManager[] trustManagers = loadTrustManagers(config);
            KeyManager[] keyManagers = loadKeyManagers(config);

            SSLContext sslContext = SSLContext.getInstance(config.sslProtocol());
            sslContext.init(keyManagers, trustManagers, null);

            LOG.info("SSL context created with protocol: {}", config.sslProtocol());
            return sslContext;
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            throw new RuntimeException("Failed to create SSL context", e);
        }
    }

    private static TrustManager[] loadTrustManagers(ConnectionFactoryConfig config) {
        String path = config.sslTruststorePath();
        if (path == null) {
            return null;
        }

        try {
            KeyStore trustStore = loadKeyStore(path, config.sslTruststorePassword(), config.sslTruststoreType());
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(trustStore);
            LOG.debug("Loaded truststore from: {}", path);
            return tmf.getTrustManagers();
        } catch (NoSuchAlgorithmException | KeyStoreException e) {
            throw new RuntimeException("Failed to initialize trust managers from: " + path, e);
        }
    }

    private static KeyManager[] loadKeyManagers(ConnectionFactoryConfig config) {
        String path = config.sslKeystorePath();
        if (path == null) {
            return null;
        }

        try {
            KeyStore keyStore = loadKeyStore(path, config.sslKeystorePassword(), config.sslKeystoreType());
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            char[] password = config.sslKeystorePassword() != null
                    ? config.sslKeystorePassword().toCharArray()
                    : null;
            try {
                kmf.init(keyStore, password);
            } finally {
                if (password != null) {
                    Arrays.fill(password, '\0');
                }
            }
            LOG.debug("Loaded keystore from: {}", path);
            return kmf.getKeyManagers();
        } catch (NoSuchAlgorithmException | KeyStoreException | UnrecoverableKeyException e) {
            throw new RuntimeException("Failed to initialize key managers from: " + path, e);
        }
    }

    private static KeyStore loadKeyStore(String path, String password, String type) {
        try (FileInputStream fis = new FileInputStream(path)) {
            KeyStore keyStore = KeyStore.getInstance(type);
            char[] passwordChars = password != null ? password.toCharArray() : null;
            try {
                keyStore.load(fis, passwordChars);
            } finally {
                if (passwordChars != null) {
                    Arrays.fill(passwordChars, '\0');
                }
            }
            return keyStore;
        } catch (KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException e) {
            throw new RuntimeException("Failed to load keystore from: " + path, e);
        }
    }
}
