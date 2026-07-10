package io.kaoto.forage.vectordb.chroma;

import io.kaoto.forage.core.util.config.ConfigStore;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for Chroma request/response logging configuration.
 *
 * <p>Logging must be disabled by default: when the configuration is unset, no
 * request/response data may be logged.
 */
@DisplayName("ChromaConfig Logging Configuration Tests")
class ChromaConfigTest {

    private static final String LOG_REQUESTS_PROPERTY = "forage.chroma.log.requests";
    private static final String LOG_RESPONSES_PROPERTY = "forage.chroma.log.responses";

    @BeforeEach
    void resetConfigStore() {
        // The ConfigStore is a JVM-wide singleton that retains previously resolved
        // values; clear it so each test observes a fresh configuration state.
        ConfigStore.getInstance().reload();
    }

    @AfterEach
    void clearSystemProperties() {
        System.clearProperty(LOG_REQUESTS_PROPERTY);
        System.clearProperty(LOG_RESPONSES_PROPERTY);
        ConfigStore.getInstance().reload();
    }

    @Test
    @DisplayName("Should disable request and response logging by default")
    void shouldDisableRequestAndResponseLoggingByDefault() {
        ChromaConfig config = new ChromaConfig();

        assertThat(config.logRequests()).isFalse();
        assertThat(config.logResponses()).isFalse();
    }

    @Test
    @DisplayName("Should enable logging only when explicitly configured")
    void shouldEnableLoggingOnlyWhenExplicitlyConfigured() {
        System.setProperty(LOG_REQUESTS_PROPERTY, "true");
        System.setProperty(LOG_RESPONSES_PROPERTY, "true");

        ChromaConfig config = new ChromaConfig();

        assertThat(config.logRequests()).isTrue();
        assertThat(config.logResponses()).isTrue();
    }

    @Test
    @DisplayName("Should keep logging disabled when explicitly disabled")
    void shouldKeepLoggingDisabledWhenExplicitlyDisabled() {
        System.setProperty(LOG_REQUESTS_PROPERTY, "false");
        System.setProperty(LOG_RESPONSES_PROPERTY, "false");

        ChromaConfig config = new ChromaConfig();

        assertThat(config.logRequests()).isFalse();
        assertThat(config.logResponses()).isFalse();
    }
}
