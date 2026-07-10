package io.kaoto.forage.models.chat.azureopenai;

import io.kaoto.forage.core.util.config.ConfigStore;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for Azure OpenAI request/response logging configuration.
 *
 * <p>Logging must be disabled by default: when the configuration is unset, no
 * request/response data (prompts, completions, potentially PII) may be logged.
 */
@DisplayName("AzureOpenAiConfig Logging Configuration Tests")
class AzureOpenAiConfigTest {

    private static final String LOG_PROPERTY = "forage.azure.openai.log.requests.and.responses";

    @BeforeEach
    void resetConfigStore() {
        // The ConfigStore is a JVM-wide singleton that retains previously resolved
        // values; clear it so each test observes a fresh configuration state.
        ConfigStore.getInstance().reload();
    }

    @AfterEach
    void clearSystemProperties() {
        System.clearProperty(LOG_PROPERTY);
        ConfigStore.getInstance().reload();
    }

    @Test
    @DisplayName("Should return null when logging configuration is unset")
    void shouldReturnNullWhenLoggingConfigurationIsUnset() {
        AzureOpenAiConfig config = new AzureOpenAiConfig();

        assertThat(config.logRequestsAndResponses()).isNull();
    }

    @Test
    @DisplayName("Should resolve unset logging configuration to disabled")
    void shouldResolveUnsetLoggingConfigurationToDisabled() {
        AzureOpenAiConfig config = new AzureOpenAiConfig();

        assertThat(AzureOpenAiProvider.resolveLogRequestsAndResponses(config.logRequestsAndResponses()))
                .isFalse();
    }

    @Test
    @DisplayName("Should resolve logging flag defensively")
    void shouldResolveLoggingFlagDefensively() {
        assertThat(AzureOpenAiProvider.resolveLogRequestsAndResponses(null)).isFalse();
        assertThat(AzureOpenAiProvider.resolveLogRequestsAndResponses(Boolean.FALSE))
                .isFalse();
        assertThat(AzureOpenAiProvider.resolveLogRequestsAndResponses(Boolean.TRUE))
                .isTrue();
    }

    @Test
    @DisplayName("Should enable logging only when explicitly configured")
    void shouldEnableLoggingOnlyWhenExplicitlyConfigured() {
        System.setProperty(LOG_PROPERTY, "true");

        AzureOpenAiConfig config = new AzureOpenAiConfig();

        assertThat(config.logRequestsAndResponses()).isTrue();
        assertThat(AzureOpenAiProvider.resolveLogRequestsAndResponses(config.logRequestsAndResponses()))
                .isTrue();
    }

    @Test
    @DisplayName("Should keep logging disabled when explicitly disabled")
    void shouldKeepLoggingDisabledWhenExplicitlyDisabled() {
        System.setProperty(LOG_PROPERTY, "false");

        AzureOpenAiConfig config = new AzureOpenAiConfig();

        assertThat(config.logRequestsAndResponses()).isFalse();
        assertThat(AzureOpenAiProvider.resolveLogRequestsAndResponses(config.logRequestsAndResponses()))
                .isFalse();
    }
}
