package io.kaoto.forage.agent;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AgentCreator Tests")
class AgentCreatorTest {

    private static final String TEST_PROVIDER = "test.provider";
    private static final String TEST_KEY = "test.key";
    private static final String FULL_KEY = "forage." + TEST_PROVIDER + "." + TEST_KEY;

    @AfterEach
    void clearTestProperty() {
        System.clearProperty(FULL_KEY);
    }

    @Nested
    @DisplayName("getProviderConfigPrefix() — drift-prevention")
    class PrefixMappingTests {

        @Test
        @DisplayName("Every model kind maps to the prefix used by its ConfigEntries class")
        void everyModelKindMapsToCorrectConfigEntriesPrefix() {
            // openai → OpenAIConfigEntries: forage.openai.*
            assertThat(AgentCreator.getProviderConfigPrefix("openai")).isEqualTo("openai");
            // ollama → OllamaConfigEntries: forage.ollama.*
            assertThat(AgentCreator.getProviderConfigPrefix("ollama")).isEqualTo("ollama");
            // google-gemini → GoogleConfigEntries: forage.google.*
            assertThat(AgentCreator.getProviderConfigPrefix("google-gemini")).isEqualTo("google");
            // azure-openai → AzureOpenAiConfigEntries: forage.azure.openai.*
            assertThat(AgentCreator.getProviderConfigPrefix("azure-openai")).isEqualTo("azure.openai");
            // anthropic → AnthropicConfigEntries: forage.anthropic.*
            assertThat(AgentCreator.getProviderConfigPrefix("anthropic")).isEqualTo("anthropic");
            // mistral-ai → MistralAiConfigEntries: forage.mistralai.*
            assertThat(AgentCreator.getProviderConfigPrefix("mistral-ai")).isEqualTo("mistralai");
            // hugging-face → HuggingFaceConfigEntries: forage.huggingface.*
            assertThat(AgentCreator.getProviderConfigPrefix("hugging-face")).isEqualTo("huggingface");
            // watsonx-ai → WatsonxAiConfigEntries: forage.watsonxai.*
            assertThat(AgentCreator.getProviderConfigPrefix("watsonx-ai")).isEqualTo("watsonxai");
            // local-ai → LocalAiConfigEntries: forage.localai.*
            assertThat(AgentCreator.getProviderConfigPrefix("local-ai")).isEqualTo("localai");
            // dashscope → DashscopeConfigEntries: forage.dashscope.*
            assertThat(AgentCreator.getProviderConfigPrefix("dashscope")).isEqualTo("dashscope");
        }

        @Test
        @DisplayName("Unknown model kinds fall back to dash-to-dot conversion")
        void unknownModelKindFallsBackToDashDotConversion() {
            assertThat(AgentCreator.getProviderConfigPrefix("some-new-provider"))
                    .isEqualTo("some.new.provider");
        }
    }

    @Nested
    @DisplayName("setSystemPropertyIfNotNull() + restoreSystemProperties()")
    class SnapshotRestoreTests {

        @Test
        @DisplayName("Previous property value is restored after set")
        void previousPropertyValueRestoredAfterSet() {
            System.setProperty(FULL_KEY, "original-value");

            Map<String, String> snapshot = new LinkedHashMap<>();
            AgentCreator.setSystemPropertyIfNotNull(snapshot, null, TEST_PROVIDER, TEST_KEY, "overridden-value");

            assertThat(System.getProperty(FULL_KEY)).isEqualTo("overridden-value");

            AgentCreator.restoreSystemProperties(snapshot);

            assertThat(System.getProperty(FULL_KEY)).isEqualTo("original-value");
        }

        @Test
        @DisplayName("Property is cleared when it was not previously set")
        void propertyIsClearedWhenNotPreviouslySet() {
            assertThat(System.getProperty(FULL_KEY)).isNull();

            Map<String, String> snapshot = new LinkedHashMap<>();
            AgentCreator.setSystemPropertyIfNotNull(snapshot, null, TEST_PROVIDER, TEST_KEY, "new-value");

            assertThat(System.getProperty(FULL_KEY)).isEqualTo("new-value");

            AgentCreator.restoreSystemProperties(snapshot);

            assertThat(System.getProperty(FULL_KEY)).isNull();
        }

        @Test
        @DisplayName("Null value does not set property and snapshot stays empty")
        void nullValueDoesNotSetProperty() {
            Map<String, String> snapshot = new LinkedHashMap<>();
            AgentCreator.setSystemPropertyIfNotNull(snapshot, null, TEST_PROVIDER, TEST_KEY, null);

            assertThat(System.getProperty(FULL_KEY)).isNull();
            assertThat(snapshot).isEmpty();
        }

        @Test
        @DisplayName("Prefixed key is built correctly")
        void prefixedKeyIsBuiltCorrectly() {
            String prefixedKey = "forage.myagent." + TEST_PROVIDER + "." + TEST_KEY;
            try {
                Map<String, String> snapshot = new LinkedHashMap<>();
                AgentCreator.setSystemPropertyIfNotNull(snapshot, "myagent", TEST_PROVIDER, TEST_KEY, "val");

                assertThat(System.getProperty(prefixedKey)).isEqualTo("val");

                AgentCreator.restoreSystemProperties(snapshot);
                assertThat(System.getProperty(prefixedKey)).isNull();
            } finally {
                System.clearProperty(prefixedKey);
            }
        }
    }

    @Nested
    @DisplayName("Embedding bridge key correctness")
    class EmbeddingBridgeKeyTests {

        @Test
        @DisplayName(
                "Embedding bridge sets forage.ollama.embedding.model.base.url not forage.ollama.embedding.base.url")
        void embeddingBridgeUsesModelBaseUrlKey() {
            String correctKey = "forage.ollama.embedding.model.base.url";
            String wrongKey = "forage.ollama.embedding.base.url";
            try {
                Map<String, String> snapshot = new LinkedHashMap<>();
                AgentCreator.setSystemPropertyIfNotNull(
                        snapshot, null, "ollama", "embedding.model.base.url", "http://remote:11434");

                assertThat(System.getProperty(correctKey)).isEqualTo("http://remote:11434");
                assertThat(System.getProperty(wrongKey)).isNull();

                AgentCreator.restoreSystemProperties(snapshot);
            } finally {
                System.clearProperty(correctKey);
                System.clearProperty(wrongKey);
            }
        }

        @Test
        @DisplayName(
                "Embedding bridge sets forage.ollama.embedding.model.max.retries not forage.ollama.embedding.max.retries")
        void embeddingBridgeUsesModelMaxRetriesKey() {
            String correctKey = "forage.ollama.embedding.model.max.retries";
            String wrongKey = "forage.ollama.embedding.max.retries";
            try {
                Map<String, String> snapshot = new LinkedHashMap<>();
                AgentCreator.setSystemPropertyIfNotNull(snapshot, null, "ollama", "embedding.model.max.retries", 3);

                assertThat(System.getProperty(correctKey)).isEqualTo("3");
                assertThat(System.getProperty(wrongKey)).isNull();

                AgentCreator.restoreSystemProperties(snapshot);
            } finally {
                System.clearProperty(correctKey);
                System.clearProperty(wrongKey);
            }
        }
    }
}
