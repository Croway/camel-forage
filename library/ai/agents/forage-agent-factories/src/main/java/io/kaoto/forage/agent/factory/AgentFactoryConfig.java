package io.kaoto.forage.agent.factory;

import java.util.List;
import io.kaoto.forage.core.util.config.AbstractConfig;
import io.kaoto.forage.core.util.config.ConfigHelper;

import static io.kaoto.forage.agent.factory.AgentFactoryConfigEntries.GUARDRAILS_INPUT;
import static io.kaoto.forage.agent.factory.AgentFactoryConfigEntries.GUARDRAILS_OUTPUT;
import static io.kaoto.forage.agent.factory.AgentFactoryConfigEntries.PROVIDER_AGENT_CLASS;
import static io.kaoto.forage.agent.factory.AgentFactoryConfigEntries.PROVIDER_FEATURES;
import static io.kaoto.forage.agent.factory.AgentFactoryConfigEntries.PROVIDER_FEATURES_MEMORY_FACTORY_CLASS;
import static io.kaoto.forage.agent.factory.AgentFactoryConfigEntries.PROVIDER_MODEL_FACTORY_CLASS;

/**
 * Configuration class for individual agent instances within the Camel Forage framework.
 *
 * <p>This class manages configuration settings for single agent implementations, providing
 * access to model providers, agent features, and memory factory configurations. It supports
 * both default and named/prefixed configurations for multi-instance scenarios.
 *
 * <p>This configuration is typically used by the MultiAgentFactory for agent setups,
 * supporting both single-agent and multi-agent scenarios.
 */
public class AgentFactoryConfig extends AbstractConfig {

    public AgentFactoryConfig() {
        this(null);
    }

    public AgentFactoryConfig(String prefix) {
        super(prefix, AgentFactoryConfigEntries.class);
    }

    @Override
    public String name() {
        return "forage-agent-factory";
    }

    public String providerModelFactoryClass() {
        return get(PROVIDER_MODEL_FACTORY_CLASS).orElse(null);
    }

    public List<String> providerFeatures() {
        return ConfigHelper.readAsList(PROVIDER_FEATURES.asNamed(prefix()));
    }

    public String providerFeaturesMemoryFactoryClass() {
        return get(PROVIDER_FEATURES_MEMORY_FACTORY_CLASS).orElse(null);
    }

    public String providerAgentClass() {
        return get(PROVIDER_AGENT_CLASS).orElse(null);
    }

    public List<String> guardrailsInput() {
        return ConfigHelper.readAsList(GUARDRAILS_INPUT.asNamed(prefix()));
    }

    public List<String> guardrailsOutput() {
        return ConfigHelper.readAsList(GUARDRAILS_OUTPUT.asNamed(prefix()));
    }
}
