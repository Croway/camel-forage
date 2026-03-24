package io.kaoto.forage.core.ai;

/**
 * If a {@link ChatMemoryBeanProvider} supports configurable message window size,
 * it should implement this interface so that the agent configuration can override
 * the default max messages value.
 */
public interface MaxMessagesAware {

    void withMaxMessages(int maxMessages);
}
