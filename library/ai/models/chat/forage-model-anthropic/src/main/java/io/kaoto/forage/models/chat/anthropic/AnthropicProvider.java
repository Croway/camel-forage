package io.kaoto.forage.models.chat.anthropic;

import java.time.Duration;
import java.util.List;
import io.kaoto.forage.core.ai.ModelProvider;
import io.kaoto.forage.core.annotations.ForageBean;
import dev.langchain4j.model.anthropic.AnthropicChatModel;
import dev.langchain4j.model.chat.ChatModel;

@ForageBean(
        value = "anthropic",
        components = {"camel-langchain4j-agent"},
        feature = "Chat Model",
        description = "Anthropic Claude models")
public class AnthropicProvider implements ModelProvider {

    @Override
    public ChatModel create(String id) {
        AnthropicConfig config = new AnthropicConfig(id);

        String apiKey = config.apiKey();
        String modelName = config.modelName();
        Double temperature = config.temperature();
        Integer maxTokens = config.maxTokens();
        Double topP = config.topP();
        Integer topK = config.topK();
        List<String> stopSequences = config.stopSequences();
        Integer timeoutSeconds = config.timeoutSeconds();
        Integer maxRetries = config.maxRetries();
        Boolean logRequestsAndResponses = config.logRequestsAndResponses();

        AnthropicChatModel.AnthropicChatModelBuilder builder =
                AnthropicChatModel.builder().apiKey(apiKey).modelName(modelName);

        if (temperature != null) {
            builder.temperature(temperature);
        }

        if (maxTokens != null) {
            builder.maxTokens(maxTokens);
        }

        if (topP != null) {
            builder.topP(topP);
        }

        if (topK != null) {
            builder.topK(topK);
        }

        if (stopSequences != null) {
            builder.stopSequences(stopSequences);
        }

        if (timeoutSeconds != null) {
            builder.timeout(Duration.ofSeconds(timeoutSeconds));
        }

        if (maxRetries != null) {
            builder.maxRetries(maxRetries);
        }

        if (logRequestsAndResponses != null) {
            builder.logRequests(logRequestsAndResponses);
            builder.logResponses(logRequestsAndResponses);
        }

        return builder.build();
    }
}
