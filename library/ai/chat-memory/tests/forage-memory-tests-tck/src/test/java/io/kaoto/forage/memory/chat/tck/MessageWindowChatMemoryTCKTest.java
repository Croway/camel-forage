package io.kaoto.forage.memory.chat.tck;

import java.util.List;
import io.kaoto.forage.core.ai.ChatMemoryBeanProvider;
import io.kaoto.forage.memory.chat.messagewindow.MessageWindowChatMemoryBeanProvider;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.ChatMemoryProvider;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class MessageWindowChatMemoryTCKTest extends ChatMemoryBeanProviderTCK {

    @Override
    protected ChatMemoryBeanProvider createChatMemoryFactory() {
        return new MessageWindowChatMemoryBeanProvider();
    }

    @Test
    void shouldIsolateStoresAcrossCreateCalls() {
        ChatMemoryBeanProvider factory = createChatMemoryFactory();

        ChatMemoryProvider provider1 = factory.create();
        ChatMemoryProvider provider2 = factory.create();

        ChatMemory memory1 = provider1.get("shared-id");
        ChatMemory memory2 = provider2.get("shared-id");

        UserMessage message = UserMessage.from("Only in provider1");
        memory1.add(message);

        List<ChatMessage> messages1 = memory1.messages();
        List<ChatMessage> messages2 = memory2.messages();

        assertThat(messages1).hasSize(1);
        assertThat(messages2).isEmpty();
    }
}
