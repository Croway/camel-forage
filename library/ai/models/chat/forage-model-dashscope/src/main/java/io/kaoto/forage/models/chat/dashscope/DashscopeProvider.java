package io.kaoto.forage.models.chat.dashscope;

import io.kaoto.forage.core.ai.ModelProvider;
import io.kaoto.forage.core.annotations.ForageBean;
import dev.langchain4j.community.model.dashscope.QwenChatModel;
import dev.langchain4j.model.chat.ChatModel;

@ForageBean(
        value = "dashscope",
        components = {"camel-langchain4j-agent"},
        feature = "Chat Model",
        description = "Alibaba Cloud Qwen models via DashScope")
public class DashscopeProvider implements ModelProvider {

    @Override
    public ChatModel create(String id) {
        DashscopeConfig config = new DashscopeConfig(id);

        String apiKey = config.apiKey();
        String modelName = config.modelName();
        Double temperature = config.temperature();
        Integer maxTokens = config.maxTokens();
        Double topP = config.topP();
        Integer topK = config.topK();
        Double repetitionPenalty = config.repetitionPenalty();
        Long seed = config.seed();
        Boolean enableSearch = config.enableSearch();

        QwenChatModel.QwenChatModelBuilder builder =
                QwenChatModel.builder().apiKey(apiKey).modelName(modelName);

        if (temperature != null) {
            builder.temperature(temperature.floatValue());
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

        if (repetitionPenalty != null) {
            builder.repetitionPenalty(repetitionPenalty.floatValue());
        }

        if (seed != null) {
            builder.seed(seed.intValue());
        }

        if (enableSearch != null) {
            builder.enableSearch(enableSearch);
        }

        return builder.build();
    }
}
