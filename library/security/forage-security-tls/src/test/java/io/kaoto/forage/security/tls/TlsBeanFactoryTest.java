package io.kaoto.forage.security.tls;

import java.util.ServiceLoader;
import org.apache.camel.support.jsse.SSLContextParameters;
import io.kaoto.forage.core.common.BeanFactory;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TlsBeanFactory Tests")
class TlsBeanFactoryTest {

    @Nested
    @DisplayName("ServiceLoader Tests")
    class ServiceLoaderTests {

        @Test
        @DisplayName("Should be discoverable via ServiceLoader")
        void shouldBeDiscoverableViaServiceLoader() {
            ServiceLoader<BeanFactory> loader = ServiceLoader.load(BeanFactory.class);

            boolean found = false;
            for (BeanFactory factory : loader) {
                if (factory instanceof TlsBeanFactory) {
                    found = true;
                    break;
                }
            }

            assertThat(found).isTrue();
        }
    }

    @Nested
    @DisplayName("SSLContextParameters Creation Tests")
    class SslContextParametersCreationTests {

        @Test
        @DisplayName("Should return null when nothing configured")
        void shouldReturnNullWhenNothingConfigured() {
            TlsBeanFactory factory = new TlsBeanFactory();
            TlsConfigTest.TestTlsConfig config = new TlsConfigTest.TestTlsConfig();

            SSLContextParameters result = factory.createSslContextParameters(config);

            assertThat(result).isNull();
        }

        @Test
        @DisplayName("Should create with keystore only")
        void shouldCreateWithKeystoreOnly() {
            TlsBeanFactory factory = new TlsBeanFactory();
            TlsConfigTest.TestTlsConfig config =
                    TlsConfigTest.TestTlsConfig.withKeystore("/path/to/keystore.p12", "secret", "PKCS12");

            SSLContextParameters result = factory.createSslContextParameters(config);

            assertThat(result).isNotNull();
            assertThat(result.getKeyManagers()).isNotNull();
            assertThat(result.getKeyManagers().getKeyStore().getResource()).isEqualTo("/path/to/keystore.p12");
            assertThat(result.getKeyManagers().getKeyStore().getType()).isEqualTo("PKCS12");
            assertThat(result.getTrustManagers()).isNull();
        }

        @Test
        @DisplayName("Should create with truststore only")
        void shouldCreateWithTruststoreOnly() {
            TlsBeanFactory factory = new TlsBeanFactory();
            TlsConfigTest.TestTlsConfig config =
                    TlsConfigTest.TestTlsConfig.withTruststore("/path/to/truststore.jks", "changeit", "JKS");

            SSLContextParameters result = factory.createSslContextParameters(config);

            assertThat(result).isNotNull();
            assertThat(result.getTrustManagers()).isNotNull();
            assertThat(result.getTrustManagers().getKeyStore().getResource()).isEqualTo("/path/to/truststore.jks");
            assertThat(result.getTrustManagers().getKeyStore().getType()).isEqualTo("JKS");
            assertThat(result.getKeyManagers()).isNull();
        }

        @Test
        @DisplayName("Should create with both keystore and truststore")
        void shouldCreateWithBothKeystoreAndTruststore() {
            TlsBeanFactory factory = new TlsBeanFactory();
            TlsConfigTest.TestTlsConfig config =
                    TlsConfigTest.TestTlsConfig.withKeystore("/path/to/keystore.p12", "secret", "PKCS12");
            config = withTruststore(config, "/path/to/truststore.jks", "changeit", "JKS");

            SSLContextParameters result = factory.createSslContextParameters(config);

            assertThat(result).isNotNull();
            assertThat(result.getKeyManagers()).isNotNull();
            assertThat(result.getTrustManagers()).isNotNull();
        }

        @Test
        @DisplayName("Should not set server parameters when client auth is NONE")
        void shouldNotSetServerParametersWhenClientAuthIsNone() {
            TlsBeanFactory factory = new TlsBeanFactory();
            TlsConfigTest.TestTlsConfig config =
                    TlsConfigTest.TestTlsConfig.withKeystore("/path/to/keystore.jks", "secret", "JKS");

            SSLContextParameters result = factory.createSslContextParameters(config);

            assertThat(result).isNotNull();
            assertThat(result.getServerParameters()).isNull();
        }

        @Test
        @DisplayName("Should set server parameters when client auth is REQUIRE")
        void shouldSetServerParametersWhenClientAuthIsRequire() {
            TlsBeanFactory factory = new TlsBeanFactory();
            TlsConfigTest.TestTlsConfig config =
                    TlsConfigTest.TestTlsConfig.withKeystore("/path/to/keystore.jks", "secret", "JKS");
            config = withClientAuth(config, "REQUIRE");

            SSLContextParameters result = factory.createSslContextParameters(config);

            assertThat(result).isNotNull();
            assertThat(result.getServerParameters()).isNotNull();
            assertThat(result.getServerParameters().getClientAuthentication()).isEqualTo("REQUIRE");
        }

        @Test
        @DisplayName("Should set cipher suites when configured")
        void shouldSetCipherSuitesWhenConfigured() {
            TlsBeanFactory factory = new TlsBeanFactory();
            TlsConfigTest.TestTlsConfig config =
                    TlsConfigTest.TestTlsConfig.withKeystore("/path/to/keystore.jks", "secret", "JKS");
            config = withCipherSuites(config, "TLS_AES_128_GCM_SHA256,TLS_AES_256_GCM_SHA384");

            SSLContextParameters result = factory.createSslContextParameters(config);

            assertThat(result).isNotNull();
            assertThat(result.getCipherSuites()).isNotNull();
            assertThat(result.getCipherSuites().getCipherSuite())
                    .containsExactly("TLS_AES_128_GCM_SHA256", "TLS_AES_256_GCM_SHA384");
        }

        @Test
        @DisplayName("Should set secure socket protocol")
        void shouldSetSecureSocketProtocol() {
            TlsBeanFactory factory = new TlsBeanFactory();
            TlsConfigTest.TestTlsConfig config =
                    TlsConfigTest.TestTlsConfig.withKeystore("/path/to/keystore.jks", "secret", "JKS");

            SSLContextParameters result = factory.createSslContextParameters(config);

            assertThat(result).isNotNull();
            assertThat(result.getSecureSocketProtocol()).isEqualTo("TLSv1.3");
        }

        private TlsConfigTest.TestTlsConfig withTruststore(
                TlsConfigTest.TestTlsConfig base, String path, String password, String type) {
            TlsConfigTest.TestTlsConfig config = TlsConfigTest.TestTlsConfig.withKeystore(
                    base.keystorePath(), base.keystorePassword(), base.keystoreType());
            // Use reflection-free approach: create a combined config
            return new CombinedTestTlsConfig(
                    base.keystorePath(),
                    base.keystorePassword(),
                    base.keystoreType(),
                    path,
                    password,
                    type,
                    base.clientAuthentication(),
                    null,
                    base.secureSocketProtocol());
        }

        private TlsConfigTest.TestTlsConfig withClientAuth(TlsConfigTest.TestTlsConfig base, String clientAuth) {
            return new CombinedTestTlsConfig(
                    base.keystorePath(),
                    base.keystorePassword(),
                    base.keystoreType(),
                    base.truststorePath(),
                    base.truststorePassword(),
                    base.truststoreType(),
                    clientAuth,
                    null,
                    base.secureSocketProtocol());
        }

        private TlsConfigTest.TestTlsConfig withCipherSuites(TlsConfigTest.TestTlsConfig base, String cipherSuites) {
            return new CombinedTestTlsConfig(
                    base.keystorePath(),
                    base.keystorePassword(),
                    base.keystoreType(),
                    base.truststorePath(),
                    base.truststorePassword(),
                    base.truststoreType(),
                    base.clientAuthentication(),
                    cipherSuites,
                    base.secureSocketProtocol());
        }
    }

    static class CombinedTestTlsConfig extends TlsConfigTest.TestTlsConfig {
        private final String ksPath, ksPassword, ksType;
        private final String tsPath, tsPassword, tsType;
        private final String clientAuth, cipherSuites, protocol;

        CombinedTestTlsConfig(
                String ksPath,
                String ksPassword,
                String ksType,
                String tsPath,
                String tsPassword,
                String tsType,
                String clientAuth,
                String cipherSuites,
                String protocol) {
            super();
            this.ksPath = ksPath;
            this.ksPassword = ksPassword;
            this.ksType = ksType;
            this.tsPath = tsPath;
            this.tsPassword = tsPassword;
            this.tsType = tsType;
            this.clientAuth = clientAuth;
            this.cipherSuites = cipherSuites;
            this.protocol = protocol;
        }

        @Override
        public String keystorePath() {
            return ksPath;
        }

        @Override
        public String keystorePassword() {
            return ksPassword;
        }

        @Override
        public String keystoreType() {
            return ksType != null ? ksType : "JKS";
        }

        @Override
        public String truststorePath() {
            return tsPath;
        }

        @Override
        public String truststorePassword() {
            return tsPassword;
        }

        @Override
        public String truststoreType() {
            return tsType != null ? tsType : "JKS";
        }

        @Override
        public String clientAuthentication() {
            return clientAuth != null ? clientAuth : "NONE";
        }

        @Override
        public java.util.Optional<String> cipherSuites() {
            return java.util.Optional.ofNullable(cipherSuites);
        }

        @Override
        public String secureSocketProtocol() {
            return protocol != null ? protocol : "TLSv1.3";
        }
    }
}
