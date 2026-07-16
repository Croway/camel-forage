package io.kaoto.forage.agent.simple;

import java.util.List;
import dev.langchain4j.data.message.Content;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface ForageAgentWithMemory {

    String chat(@MemoryId Object memoryId, @UserMessage String message);

    String chat(@MemoryId Object memoryId, @UserMessage String message, @UserMessage List<Content> contents);

    @SystemMessage("{{prompt}}")
    String chat(@MemoryId Object memoryId, @UserMessage String message, @V("prompt") String prompt);

    @SystemMessage("{{prompt}}")
    String chat(
            @MemoryId Object memoryId,
            @UserMessage String message,
            @UserMessage List<Content> contents,
            @V("prompt") String prompt);
}
