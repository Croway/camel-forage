package io.kaoto.forage.security.tls;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TlsConfig Tests")
class TlsConfigTest {

    @Nested
    @DisplayName("Config Name Tests")
    class ConfigNameTests {

        @Test
        @DisplayName("Should return correct config name")
        void shouldReturnCorrectConfigName() {
            TestTlsConfig config = new TestTlsConfig();

            assertThat(config.name()).isEqualTo("forage-security-tls");
        }
    }

    @Nested
    @DisplayName("Keystore Tests")
    class KeystoreTests {

        @Test
        @DisplayName("Should return null when keystore path not configured")
        void shouldReturnNullWhenKeystorePathNotConfigured() {
            TestTlsConfig config = new TestTlsConfig();

            assertThat(config.keystorePath()).isNull();
        }

        @Test
        @DisplayName("Should return configured keystore path")
        void shouldReturnConfiguredKeystorePath() {
            TestTlsConfig config = TestTlsConfig.withKeystore("/path/to/keystore.p12", "secret", "PKCS12");

            assertThat(config.keystorePath()).isEqualTo("/path/to/keystore.p12");
            assertThat(config.keystorePassword()).isEqualTo("secret");
            assertThat(config.keystoreType()).isEqualTo("PKCS12");
        }

        @Test
        @DisplayName("Should default keystore type to JKS")
        void shouldDefaultKeystoreTypeToJKS() {
            TestTlsConfig config = new TestTlsConfig();

            assertThat(config.keystoreType()).isEqualTo("JKS");
        }
    }

    @Nested
    @DisplayName("Truststore Tests")
    class TruststoreTests {

        @Test
        @DisplayName("Should return null when truststore path not configured")
        void shouldReturnNullWhenTruststorePathNotConfigured() {
            TestTlsConfig config = new TestTlsConfig();

            assertThat(config.truststorePath()).isNull();
        }

        @Test
        @DisplayName("Should return configured truststore path")
        void shouldReturnConfiguredTruststorePath() {
            TestTlsConfig config = TestTlsConfig.withTruststore("/path/to/truststore.jks", "changeit", "JKS");

            assertThat(config.truststorePath()).isEqualTo("/path/to/truststore.jks");
            assertThat(config.truststorePassword()).isEqualTo("changeit");
            assertThat(config.truststoreType()).isEqualTo("JKS");
        }

        @Test
        @DisplayName("Should default truststore type to JKS")
        void shouldDefaultTruststoreTypeToJKS() {
            TestTlsConfig config = new TestTlsConfig();

            assertThat(config.truststoreType()).isEqualTo("JKS");
        }
    }

    @Nested
    @DisplayName("Client Authentication Tests")
    class ClientAuthenticationTests {

        @Test
        @DisplayName("Should default to NONE")
        void shouldDefaultToNone() {
            TestTlsConfig config = new TestTlsConfig();

            assertThat(config.clientAuthentication()).isEqualTo("NONE");
        }

        @Test
        @DisplayName("Should return configured value")
        void shouldReturnConfiguredValue() {
            TestTlsConfig config = TestTlsConfig.withClientAuthentication("REQUIRE");

            assertThat(config.clientAuthentication()).isEqualTo("REQUIRE");
        }
    }

    @Nested
    @DisplayName("Cipher Suites Tests")
    class CipherSuitesTests {

        @Test
        @DisplayName("Should return empty when not configured")
        void shouldReturnEmptyWhenNotConfigured() {
            TestTlsConfig config = new TestTlsConfig();

            assertThat(config.cipherSuites()).isEmpty();
            assertThat(config.cipherSuitesList()).isEmpty();
        }

        @Test
        @DisplayName("Should parse comma-separated cipher suites")
        void shouldParseCommaSeparatedCipherSuites() {
            TestTlsConfig config = TestTlsConfig.withCipherSuites("TLS_AES_128_GCM_SHA256, TLS_AES_256_GCM_SHA384");

            assertThat(config.cipherSuitesList()).containsExactly("TLS_AES_128_GCM_SHA256", "TLS_AES_256_GCM_SHA384");
        }
    }

    @Nested
    @DisplayName("Secure Socket Protocol Tests")
    class SecureSocketProtocolTests {

        @Test
        @DisplayName("Should default to TLSv1.3")
        void shouldDefaultToTLSv13() {
            TestTlsConfig config = new TestTlsConfig();

            assertThat(config.secureSocketProtocol()).isEqualTo("TLSv1.3");
        }

        @Test
        @DisplayName("Should return configured value")
        void shouldReturnConfiguredValue() {
            TestTlsConfig config = TestTlsConfig.withSecureSocketProtocol("TLSv1.2");

            assertThat(config.secureSocketProtocol()).isEqualTo("TLSv1.2");
        }
    }

    static class TestTlsConfig extends TlsConfig {
        private String testKeystorePath;
        private String testKeystorePassword;
        private String testKeystoreType;
        private String testTruststorePath;
        private String testTruststorePassword;
        private String testTruststoreType;
        private String testClientAuthentication;
        private String testCipherSuites;
        private String testSecureSocketProtocol;

        TestTlsConfig() {
            super();
        }

        static TestTlsConfig withKeystore(String path, String password, String type) {
            TestTlsConfig config = new TestTlsConfig();
            config.testKeystorePath = path;
            config.testKeystorePassword = password;
            config.testKeystoreType = type;
            return config;
        }

        static TestTlsConfig withTruststore(String path, String password, String type) {
            TestTlsConfig config = new TestTlsConfig();
            config.testTruststorePath = path;
            config.testTruststorePassword = password;
            config.testTruststoreType = type;
            return config;
        }

        static TestTlsConfig withClientAuthentication(String clientAuth) {
            TestTlsConfig config = new TestTlsConfig();
            config.testClientAuthentication = clientAuth;
            return config;
        }

        static TestTlsConfig withCipherSuites(String cipherSuites) {
            TestTlsConfig config = new TestTlsConfig();
            config.testCipherSuites = cipherSuites;
            return config;
        }

        static TestTlsConfig withSecureSocketProtocol(String protocol) {
            TestTlsConfig config = new TestTlsConfig();
            config.testSecureSocketProtocol = protocol;
            return config;
        }

        @Override
        public String keystorePath() {
            return testKeystorePath;
        }

        @Override
        public String keystorePassword() {
            return testKeystorePassword;
        }

        @Override
        public String keystoreType() {
            return testKeystoreType != null ? testKeystoreType : "JKS";
        }

        @Override
        public String truststorePath() {
            return testTruststorePath;
        }

        @Override
        public String truststorePassword() {
            return testTruststorePassword;
        }

        @Override
        public String truststoreType() {
            return testTruststoreType != null ? testTruststoreType : "JKS";
        }

        @Override
        public String clientAuthentication() {
            return testClientAuthentication != null ? testClientAuthentication : "NONE";
        }

        @Override
        public Optional<String> cipherSuites() {
            return Optional.ofNullable(testCipherSuites);
        }

        @Override
        public List<String> cipherSuitesList() {
            return cipherSuites()
                    .map(s -> java.util.Arrays.stream(s.split(","))
                            .map(String::trim)
                            .filter(v -> !v.isEmpty())
                            .collect(java.util.stream.Collectors.toList()))
                    .orElse(Collections.emptyList());
        }

        @Override
        public String secureSocketProtocol() {
            return testSecureSocketProtocol != null ? testSecureSocketProtocol : "TLSv1.3";
        }
    }
}
