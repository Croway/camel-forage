package io.kaoto.forage.vectordb.weaviate;

import io.kaoto.forage.core.util.config.ConfigStore;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for WeaviateConfig, focusing on ensuring sensitive values such as the
 * API key are never exposed through {@link WeaviateConfig#toString()}.
 */
@DisplayName("WeaviateConfig Tests")
class WeaviateConfigTest {

    private static final String API_KEY_PROPERTY = "forage.weaviate.api.key";
    private static final String SECRET_API_KEY = "super-secret-api-key-value";

    @BeforeEach
    void setUpRequiredProperties() {
        // The ConfigStore is a JVM-wide singleton that retains previously resolved
        // values; clear it so each test observes a fresh configuration state.
        ConfigStore.getInstance().reload();

        System.setProperty("forage.weaviate.scheme", "http");
        System.setProperty("forage.weaviate.host", "localhost");
        System.setProperty("forage.weaviate.port", "8080");
        System.setProperty("forage.weaviate.use.grpc.for.inserts", "false");
        System.setProperty("forage.weaviate.secured.grpc", "false");
        System.setProperty("forage.weaviate.object.class", "TestVectors");
    }

    @AfterEach
    void clearSystemProperties() {
        System.clearProperty(API_KEY_PROPERTY);
        System.clearProperty("forage.weaviate.scheme");
        System.clearProperty("forage.weaviate.host");
        System.clearProperty("forage.weaviate.port");
        System.clearProperty("forage.weaviate.use.grpc.for.inserts");
        System.clearProperty("forage.weaviate.secured.grpc");
        System.clearProperty("forage.weaviate.object.class");
        ConfigStore.getInstance().reload();
    }

    @Test
    @DisplayName("Should mask the API key in toString")
    void shouldMaskApiKeyInToString() {
        System.setProperty(API_KEY_PROPERTY, SECRET_API_KEY);

        WeaviateConfig config = new WeaviateConfig();

        assertThat(config.apiKey()).isEqualTo(SECRET_API_KEY);
        assertThat(config.toString())
                .doesNotContain(SECRET_API_KEY)
                .contains("apiKey: ***")
                .contains("host localhost")
                .contains("objectClass TestVectors");
    }

    @Test
    @DisplayName("Should print null for an unset API key in toString")
    void shouldPrintNullForUnsetApiKeyInToString() {
        WeaviateConfig config = new WeaviateConfig();

        assertThat(config.apiKey()).isNull();
        assertThat(config.toString()).contains("apiKey: null");
    }
}
