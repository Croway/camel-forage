package io.kaoto.forage.security.tls;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import io.kaoto.forage.core.util.config.AbstractConfig;

import static io.kaoto.forage.security.tls.TlsConfigEntries.CIPHER_SUITES;
import static io.kaoto.forage.security.tls.TlsConfigEntries.CLIENT_AUTHENTICATION;
import static io.kaoto.forage.security.tls.TlsConfigEntries.KEYSTORE_PASSWORD;
import static io.kaoto.forage.security.tls.TlsConfigEntries.KEYSTORE_PATH;
import static io.kaoto.forage.security.tls.TlsConfigEntries.KEYSTORE_TYPE;
import static io.kaoto.forage.security.tls.TlsConfigEntries.SECURE_SOCKET_PROTOCOL;
import static io.kaoto.forage.security.tls.TlsConfigEntries.TRUSTSTORE_PASSWORD;
import static io.kaoto.forage.security.tls.TlsConfigEntries.TRUSTSTORE_PATH;
import static io.kaoto.forage.security.tls.TlsConfigEntries.TRUSTSTORE_TYPE;

public class TlsConfig extends AbstractConfig {

    public TlsConfig() {
        this(null);
    }

    public TlsConfig(String prefix) {
        super(prefix, TlsConfigEntries.class);
    }

    @Override
    public String name() {
        return "forage-security-tls";
    }

    public String keystorePath() {
        return get(KEYSTORE_PATH).orElse(null);
    }

    public String keystorePassword() {
        return get(KEYSTORE_PASSWORD).orElse(null);
    }

    public String keystoreType() {
        return get(KEYSTORE_TYPE).orElse(KEYSTORE_TYPE.defaultValue());
    }

    public String truststorePath() {
        return get(TRUSTSTORE_PATH).orElse(null);
    }

    public String truststorePassword() {
        return get(TRUSTSTORE_PASSWORD).orElse(null);
    }

    public String truststoreType() {
        return get(TRUSTSTORE_TYPE).orElse(TRUSTSTORE_TYPE.defaultValue());
    }

    public String clientAuthentication() {
        return get(CLIENT_AUTHENTICATION).orElse(CLIENT_AUTHENTICATION.defaultValue());
    }

    public Optional<String> cipherSuites() {
        return get(CIPHER_SUITES);
    }

    public List<String> cipherSuitesList() {
        return cipherSuites()
                .map(s -> Arrays.stream(s.split(","))
                        .map(String::trim)
                        .filter(v -> !v.isEmpty())
                        .collect(Collectors.toList()))
                .orElse(Collections.emptyList());
    }

    public String secureSocketProtocol() {
        return get(SECURE_SOCKET_PROTOCOL).orElse(SECURE_SOCKET_PROTOCOL.defaultValue());
    }
}
