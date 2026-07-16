package io.kaoto.forage.models.chat.google;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.kaoto.forage.core.ai.ModelProvider;
import io.kaoto.forage.core.annotations.ForageBean;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;

import static java.time.Duration.ofSeconds;

/**
 * Provider for creating Google Gemini chat models
 */
@ForageBean(
        value = "google-gemini",
        components = {"camel-langchain4j-agent"},
        feature = "Chat Model",
        description = "Google Gemini models")
public class GoogleGeminiProvider implements ModelProvider {
    private static final Logger LOG = LoggerFactory.getLogger(GoogleGeminiProvider.class);

    @Override
    public ChatModel create(String id) {
        final GoogleConfig config = new GoogleConfig(id);
        LOG.trace("Creating google chat model");

        GoogleAiGeminiChatModel.GoogleAiGeminiChatModelBuilder builder =
                GoogleAiGeminiChatModel.builder().apiKey(config.apiKey()).modelName(config.modelName());

        Double temperature = config.temperature();
        if (temperature != null) {
            builder.temperature(temperature);
        }

        Integer timeout = config.timeout();
        if (timeout != null) {
            builder.timeout(ofSeconds(timeout));
        }

        if (config.maxOutputTokens() != null) {
            builder.maxOutputTokens(config.maxOutputTokens());
        }
        if (config.topP() != null) {
            builder.topP(config.topP());
        }
        if (config.topK() != null) {
            builder.topK(config.topK());
        }

        Boolean logRequests = config.logRequestsAndResponses();
        if (logRequests != null) {
            builder.logRequestsAndResponses(logRequests);
        }

        return builder.build();
    }
}
