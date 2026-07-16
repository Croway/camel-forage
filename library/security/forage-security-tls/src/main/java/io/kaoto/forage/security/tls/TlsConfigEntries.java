package io.kaoto.forage.security.tls;

import io.kaoto.forage.core.util.config.ConfigEntries;
import io.kaoto.forage.core.util.config.ConfigModule;
import io.kaoto.forage.core.util.config.ConfigTag;

public final class TlsConfigEntries extends ConfigEntries {

    public static final ConfigModule KEYSTORE_PATH = ConfigModule.of(
            TlsConfig.class,
            "forage.tls.keystore.path",
            "Path to the keystore file (filesystem path or classpath: URI)",
            "Keystore Path",
            null,
            "string",
            false,
            ConfigTag.SECURITY);

    public static final ConfigModule KEYSTORE_PASSWORD = ConfigModule.of(
            TlsConfig.class,
            "forage.tls.keystore.password",
            "Password for the keystore",
            "Keystore Password",
            null,
            "password",
            false,
            ConfigTag.SECURITY);

    public static final ConfigModule KEYSTORE_TYPE = ConfigModule.of(
            TlsConfig.class,
            "forage.tls.keystore.type",
            "Type of the keystore (JKS, PKCS12, etc.)",
            "Keystore Type",
            "JKS",
            "string",
            false,
            ConfigTag.COMMON);

    public static final ConfigModule TRUSTSTORE_PATH = ConfigModule.of(
            TlsConfig.class,
            "forage.tls.truststore.path",
            "Path to the truststore file (filesystem path or classpath: URI)",
            "Truststore Path",
            null,
            "string",
            false,
            ConfigTag.SECURITY);

    public static final ConfigModule TRUSTSTORE_PASSWORD = ConfigModule.of(
            TlsConfig.class,
            "forage.tls.truststore.password",
            "Password for the truststore",
            "Truststore Password",
            null,
            "password",
            false,
            ConfigTag.SECURITY);

    public static final ConfigModule TRUSTSTORE_TYPE = ConfigModule.of(
            TlsConfig.class,
            "forage.tls.truststore.type",
            "Type of the truststore (JKS, PKCS12, etc.)",
            "Truststore Type",
            "JKS",
            "string",
            false,
            ConfigTag.COMMON);

    public static final ConfigModule CLIENT_AUTHENTICATION = ConfigModule.of(
            TlsConfig.class,
            "forage.tls.client.authentication",
            "Client authentication mode: NONE, WANT, or REQUIRE",
            "Client Authentication",
            "NONE",
            "string",
            false,
            ConfigTag.COMMON);

    public static final ConfigModule CIPHER_SUITES = ConfigModule.of(
            TlsConfig.class,
            "forage.tls.cipher.suites",
            "Comma-separated list of cipher suites to include",
            "Cipher Suites",
            null,
            "string",
            false,
            ConfigTag.ADVANCED);

    public static final ConfigModule SECURE_SOCKET_PROTOCOL = ConfigModule.of(
            TlsConfig.class,
            "forage.tls.secure.socket.protocol",
            "The secure socket protocol (e.g., TLSv1.2, TLSv1.3)",
            "Secure Socket Protocol",
            "TLSv1.3",
            "string",
            false,
            ConfigTag.COMMON);

    static {
        initModules(
                TlsConfigEntries.class,
                KEYSTORE_PATH,
                KEYSTORE_PASSWORD,
                KEYSTORE_TYPE,
                TRUSTSTORE_PATH,
                TRUSTSTORE_PASSWORD,
                TRUSTSTORE_TYPE,
                CLIENT_AUTHENTICATION,
                CIPHER_SUITES,
                SECURE_SOCKET_PROTOCOL);
    }
}
