package io.kaoto.forage.models.chat.huggingface;

import io.kaoto.forage.core.util.config.AbstractConfig;

import static io.kaoto.forage.models.chat.huggingface.HuggingFaceConfigEntries.API_KEY;
import static io.kaoto.forage.models.chat.huggingface.HuggingFaceConfigEntries.MAX_NEW_TOKENS;
import static io.kaoto.forage.models.chat.huggingface.HuggingFaceConfigEntries.MODEL_ID;
import static io.kaoto.forage.models.chat.huggingface.HuggingFaceConfigEntries.RETURN_FULL_TEXT;
import static io.kaoto.forage.models.chat.huggingface.HuggingFaceConfigEntries.TEMPERATURE;
import static io.kaoto.forage.models.chat.huggingface.HuggingFaceConfigEntries.TIMEOUT;
import static io.kaoto.forage.models.chat.huggingface.HuggingFaceConfigEntries.WAIT_FOR_MODEL;

/**
 * Configuration class for HuggingFace integration in the Camel Forage framework.
 *
 * <p>This configuration class manages the settings required to connect to and use HuggingFace
 * Inference API for chat models. It handles authentication credentials and model configuration
 * through environment variables with appropriate fallback mechanisms.
 *
 * <p><strong>Required Configuration:</strong>
 * <ul>
 *   <li><strong>HUGGINGFACE_API_KEY</strong> - Your HuggingFace API key for authentication</li>
 * </ul>
 *
 * <p><strong>Optional Configuration:</strong>
 * <ul>
 *   <li><strong>HUGGINGFACE_MODEL_ID</strong> - The HuggingFace model ID to use (e.g., microsoft/DialoGPT-medium)</li>
 *   <li><strong>HUGGINGFACE_TEMPERATURE</strong> - Temperature for response generation (0.0-2.0)</li>
 *   <li><strong>HUGGINGFACE_MAX_NEW_TOKENS</strong> - Maximum number of new tokens to generate</li>
 *   <li><strong>HUGGINGFACE_RETURN_FULL_TEXT</strong> - Whether to return full text including input</li>
 *   <li><strong>HUGGINGFACE_WAIT_FOR_MODEL</strong> - Whether to wait for model to load</li>
 *   <li><strong>HUGGINGFACE_TIMEOUT</strong> - Request timeout in seconds</li>
 * </ul>
 *
 * <p><strong>Configuration Sources:</strong>
 * Configuration values are resolved in the following order of precedence:
 * <ol>
 *   <li>Environment variables (HUGGINGFACE_API_KEY, HUGGINGFACE_MODEL_ID, etc.)</li>
 *   <li>System properties (huggingface.api.key, huggingface.model.id, etc.)</li>
 *   <li>forage-model-hugging-face.properties file in classpath</li>
 * </ol>
 *
 * <p><strong>Usage Example:</strong>
 * <pre>{@code
 * // Set environment variables
 * export HUGGINGFACE_API_KEY="hf_your-api-key-here"
 * export HUGGINGFACE_MODEL_ID="microsoft/DialoGPT-medium"
 *
 * // Create and use configuration
 * HuggingFaceConfig config = new HuggingFaceConfig();
 * String apiKey = config.apiKey();     // Returns the configured API key
 * String modelId = config.modelId();   // Returns the configured model ID
 * }</pre>
 *
 * <p><strong>Security Considerations:</strong>
 * The API key is sensitive information and should be properly secured. Never commit API keys
 * to version control. Use environment variables or secure configuration management systems
 * in production environments.
 *
 * @see AbstractConfig
 * @see io.kaoto.forage.core.util.config.ConfigStore
 * @see io.kaoto.forage.core.util.config.ConfigModule
 * @since 1.0
 */
public class HuggingFaceConfig extends AbstractConfig {

    /**
     * Constructs a new HuggingFaceConfig and registers configuration parameters with the ConfigStore.
     */
    public HuggingFaceConfig() {
        this(null);
    }

    public HuggingFaceConfig(String prefix) {
        super(prefix, HuggingFaceConfigEntries.class);
    }

    /**
     * Returns the unique identifier for this HuggingFace configuration module.
     *
     * @return the module name "forage-model-hugging-face"
     */
    @Override
    public String name() {
        return "forage-model-hugging-face";
    }

    /**
     * Returns the HuggingFace API key for authentication.
     *
     * <p><strong>Configuration Sources (in order of precedence):</strong>
     * <ol>
     *   <li>HUGGINGFACE_API_KEY environment variable</li>
     *   <li>huggingface.api.key system property</li>
     *   <li>api-key property in forage-model-hugging-face.properties</li>
     * </ol>
     *
     * @return the HuggingFace API key
     * @throws io.kaoto.forage.core.util.config.MissingConfigException if no API key is configured
     */
    public String apiKey() {
        return getRequired(API_KEY, "Missing HuggingFace API key");
    }

    /**
     * Returns the HuggingFace model ID to use.
     *
     * <p><strong>Common Model Examples:</strong>
     * <ul>
     *   <li><strong>microsoft/DialoGPT-medium</strong> - Conversational AI model</li>
     *   <li><strong>microsoft/DialoGPT-large</strong> - Larger conversational AI model</li>
     *   <li><strong>facebook/blenderbot-400M-distill</strong> - Facebook's BlenderBot</li>
     *   <li><strong>google/flan-t5-base</strong> - Google's FLAN-T5 model</li>
     * </ul>
     *
     * @return the HuggingFace model ID, or null if not configured
     */
    public String modelId() {
        return get(MODEL_ID).orElse(null);
    }

    /**
     * Returns the temperature setting for response generation.
     *
     * <p>Temperature controls the randomness of the model's output. Lower values make responses
     * more focused and deterministic, while higher values make them more creative and varied.
     *
     * <p><strong>Value Range:</strong> 0.0 to 2.0
     * <ul>
     *   <li><strong>0.0</strong> - Most deterministic, focused responses</li>
     *   <li><strong>0.7</strong> - Balanced creativity and focus</li>
     *   <li><strong>1.0</strong> - Default setting</li>
     *   <li><strong>2.0</strong> - Maximum creativity and randomness</li>
     * </ul>
     *
     * @return the temperature value, or null if not configured
     */
    public Double temperature() {
        return get(TEMPERATURE).map(Double::parseDouble).orElse(null);
    }

    /**
     * Returns the maximum number of new tokens to generate.
     *
     * <p>This setting limits the length of the model's response, controlling only the new tokens
     * generated by the model (not including the input prompt tokens).
     *
     * @return the maximum new tokens limit, or null if not configured
     */
    public Integer maxNewTokens() {
        return get(MAX_NEW_TOKENS).map(Integer::parseInt).orElse(null);
    }

    /**
     * Returns whether to return the full text including the input prompt.
     *
     * <p>When set to true, the response includes both the input prompt and the generated text.
     * When set to false, only the newly generated text is returned.
     *
     * @return true to return full text, false to return only generated text, or null if not configured
     */
    public Boolean returnFullText() {
        return get(RETURN_FULL_TEXT).map(Boolean::parseBoolean).orElse(null);
    }

    /**
     * Returns whether to wait for the model to load if it's not ready.
     *
     * <p>When set to true, the request will wait for the model to load if it's currently
     * loading or cold. When set to false, the request may fail if the model is not ready.
     *
     * @return true to wait for model, false to fail fast, or null if not configured
     */
    public Boolean waitForModel() {
        return get(WAIT_FOR_MODEL).map(Boolean::parseBoolean).orElse(null);
    }

    /**
     * Returns the request timeout duration in seconds.
     *
     * <p>Specifies how long to wait for a response from the HuggingFace Inference API
     * before timing out the request.
     *
     * @return the timeout in seconds, or null if not configured
     */
    public Integer timeoutSeconds() {
        return get(TIMEOUT).map(Integer::parseInt).orElse(null);
    }
}
