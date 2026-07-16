package io.kaoto.forage.agent.factory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.component.langchain4j.agent.api.Agent;
import org.apache.camel.component.langchain4j.agent.api.AgentFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.kaoto.forage.core.ai.ChatMemoryBeanProvider;
import io.kaoto.forage.core.ai.ModelProvider;
import io.kaoto.forage.core.annotations.ForageBean;
import io.kaoto.forage.core.common.ServiceLoaderHelper;
import io.kaoto.forage.core.exceptions.RuntimeForageException;
import io.kaoto.forage.core.guardrails.InputGuardrailProvider;
import io.kaoto.forage.core.guardrails.OutputGuardrailProvider;
import io.kaoto.forage.core.util.config.ConfigStore;
import dev.langchain4j.guardrail.InputGuardrail;
import dev.langchain4j.guardrail.OutputGuardrail;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.model.chat.ChatModel;

/**
 * Implementation of AgentFactory that uses ServiceLoader to discover and create multiple agents
 */
public class MultiAgentFactory implements AgentFactory {
    private static final Logger LOG = LoggerFactory.getLogger(MultiAgentFactory.class);

    private CamelContext camelContext;
    private final MultiAgentConfig config = new MultiAgentConfig();

    private record AgentPair(AgentFactoryConfig agentFactoryConfig, Agent agent) {}

    private final Map<String, AgentPair> agents = new ConcurrentHashMap<>();

    public MultiAgentFactory() {
        LOG.trace("Creating MultiAgentFactory");
    }

    @Override
    public void setCamelContext(CamelContext camelContext) {
        this.camelContext = camelContext;

        ConfigStore.getInstance().setClassLoader(camelContext.getApplicationContextClassLoader());
    }

    @Override
    public CamelContext getCamelContext() {
        return camelContext;
    }

    private List<ServiceLoader.Provider<ModelProvider>> findModelProviders() {
        ServiceLoader<ModelProvider> modelLoader =
                ServiceLoader.load(ModelProvider.class, camelContext.getApplicationContextClassLoader());

        return modelLoader.stream().toList();
    }

    private List<ServiceLoader.Provider<Agent>> findAgents() {
        ServiceLoader<Agent> serviceLoader =
                ServiceLoader.load(Agent.class, camelContext.getApplicationContextClassLoader());

        return serviceLoader.stream().toList();
    }

    private List<ServiceLoader.Provider<ChatMemoryBeanProvider>> findChatMemoryFactories() {
        ServiceLoader<ChatMemoryBeanProvider> serviceLoader =
                ServiceLoader.load(ChatMemoryBeanProvider.class, camelContext.getApplicationContextClassLoader());

        return serviceLoader.stream().toList();
    }

    public Agent createAgent(Exchange exchange, String agentId) throws Exception {
        if (agentId == null) {
            throw AgentIdSelectorHelper.newUndefinedAgentException(config, exchange);
        }

        if (LOG.isTraceEnabled()) {
            LOG.debug("Available agents: {}", agents);
        }

        AgentPair cached = agents.get(agentId);
        if (cached != null) {
            LOG.debug("Reusing existing Agent for {}", agentId);
            return cached.agent;
        }

        final List<String> definedAgents = config.multiAgentNames();

        if (!definedAgents.contains(agentId)) {
            throw AgentIdSelectorHelper.newUndefinedAgentException(config, exchange);
        }

        AgentPair created = agents.computeIfAbsent(agentId, id -> {
            LOG.info("Creating new Agent for {}", id);
            AgentFactoryConfig aFactoryConfig = new AgentFactoryConfig(id);
            LOG.info("Using factory {} for {}", aFactoryConfig.name(), id);

            Agent agent = newAgent(aFactoryConfig, id);
            if (agent == null) {
                throw new RuntimeForageException(
                        "Failed to create agent for '%s': no agent provider found for class '%s'"
                                .formatted(id, aFactoryConfig.providerAgentClass()));
            }

            LOG.info("Using agent {} for {}", agent, id);
            return new AgentPair(aFactoryConfig, agent);
        });

        return created.agent;
    }

    public Agent createAgent(Exchange exchange) throws Exception {
        final String agentId = AgentIdSelectorHelper.select(config, exchange);

        return createAgent(exchange, agentId);
    }

    private Agent newAgent(AgentFactoryConfig agentFactoryConfig, String name) {
        final String agentFactoryClass = agentFactoryConfig.providerAgentClass();
        LOG.info("Creating Agent of type {}", agentFactoryClass);

        final List<ServiceLoader.Provider<Agent>> providers = findAgents();

        final ServiceLoader.Provider<Agent> agentProvider =
                ServiceLoaderHelper.findProviderByClassName(providers, agentFactoryClass);

        if (agentProvider == null) {
            LOG.warn("Agent {} has no provider for {}", name, agentFactoryClass);
            return null;
        }

        return doCreateAgent(agentProvider, agentFactoryConfig, name);
    }

    private ModelProvider newModelProvider(AgentFactoryConfig agentFactoryConfig) {
        final String modelFactoryClass = agentFactoryConfig.providerModelFactoryClass();
        LOG.trace("Creating ModelProvider of type {}", modelFactoryClass);

        final List<ServiceLoader.Provider<ModelProvider>> providers = findModelProviders();
        final ServiceLoader.Provider<ModelProvider> modelProvider =
                ServiceLoaderHelper.findProviderByClassName(providers, modelFactoryClass);

        if (modelProvider == null) {
            return null;
        }

        return modelProvider.get();
    }

    private ChatMemoryBeanProvider newChatMemoryFactory(AgentFactoryConfig agentFactoryConfig) {
        final String chatFactoryClass = agentFactoryConfig.providerFeaturesMemoryFactoryClass();
        LOG.trace("Creating ChatMemoryFactory of type {}", chatFactoryClass);
        final List<ServiceLoader.Provider<ChatMemoryBeanProvider>> providers = findChatMemoryFactories();

        final ServiceLoader.Provider<ChatMemoryBeanProvider> chatMemoryFactoryProvider =
                ServiceLoaderHelper.findProviderByClassName(providers, chatFactoryClass);

        if (chatMemoryFactoryProvider == null) {
            return null;
        }

        return chatMemoryFactoryProvider.get();
    }

    private Agent doCreateAgent(
            ServiceLoader.Provider<Agent> provider, AgentFactoryConfig agentFactoryConfig, String agentId) {
        final Agent agent = provider.get();

        if (agent instanceof ConfigurationAware configurationAware) {
            LOG.trace("Creating the model for agent '{}'", agentId);
            ModelProvider modelProvider = newModelProvider(agentFactoryConfig);
            if (modelProvider == null) {
                throw new RuntimeForageException("A model must be provided for using agent '%s'".formatted(agentId));
            }

            final ChatModel chatModel = modelProvider.create(agentId);
            final List<String> features = agentFactoryConfig.providerFeatures();

            ChatMemoryProvider chatMemoryProvider = null;
            if (features.contains(AgentFactoryConfigEntries.FEATURE_MEMORY)) {
                LOG.trace("Creating memory for agent '{}'", agentId);
                final ChatMemoryBeanProvider chatMemoryBeanProvider = newChatMemoryFactory(agentFactoryConfig);
                if (chatMemoryBeanProvider != null) {
                    chatMemoryProvider = chatMemoryBeanProvider.create(agentId);
                }
            }

            ForageAgentConfiguration agentConfiguration = new ForageAgentConfiguration();
            agentConfiguration.withChatModel(chatModel).withChatMemoryProvider(chatMemoryProvider);

            ClassLoader classLoader = camelContext.getApplicationContextClassLoader();

            List<InputGuardrail> inputGuardrails =
                    loadInputGuardrails(agentFactoryConfig.guardrailsInput(), classLoader);
            if (!inputGuardrails.isEmpty()) {
                agentConfiguration.withInputGuardrails(inputGuardrails);
            }

            List<OutputGuardrail> outputGuardrails =
                    loadOutputGuardrails(agentFactoryConfig.guardrailsOutput(), classLoader);
            if (!outputGuardrails.isEmpty()) {
                agentConfiguration.withOutputGuardrails(outputGuardrails);
            }

            configurationAware.configure(agentConfiguration);
        }

        return agent;
    }

    private List<InputGuardrail> loadInputGuardrails(List<String> selectedNames, ClassLoader classLoader) {
        if (selectedNames == null || selectedNames.isEmpty()) {
            return List.of();
        }

        ServiceLoader<InputGuardrailProvider> loader = ServiceLoader.load(InputGuardrailProvider.class, classLoader);
        List<ServiceLoader.Provider<InputGuardrailProvider>> providers =
                loader.stream().toList();

        List<InputGuardrail> guardrails = new ArrayList<>();
        for (String name : selectedNames) {
            String trimmedName = name.trim();
            InputGuardrailProvider matched = null;
            for (ServiceLoader.Provider<InputGuardrailProvider> provider : providers) {
                ForageBean annotation = provider.type().getAnnotation(ForageBean.class);
                if (annotation != null && annotation.value().equals(trimmedName)) {
                    matched = provider.get();
                    break;
                }
            }
            if (matched == null) {
                throw new RuntimeForageException(
                        "No input guardrail provider found for name '%s'".formatted(trimmedName));
            }
            LOG.info("Creating input guardrail '{}'", trimmedName);
            InputGuardrail guardrail = matched.create(null);
            if (guardrail != null) {
                guardrails.add(guardrail);
            }
        }
        return guardrails;
    }

    private List<OutputGuardrail> loadOutputGuardrails(List<String> selectedNames, ClassLoader classLoader) {
        if (selectedNames == null || selectedNames.isEmpty()) {
            return List.of();
        }

        ServiceLoader<OutputGuardrailProvider> loader = ServiceLoader.load(OutputGuardrailProvider.class, classLoader);
        List<ServiceLoader.Provider<OutputGuardrailProvider>> providers =
                loader.stream().toList();

        List<OutputGuardrail> guardrails = new ArrayList<>();
        for (String name : selectedNames) {
            String trimmedName = name.trim();
            OutputGuardrailProvider matched = null;
            for (ServiceLoader.Provider<OutputGuardrailProvider> provider : providers) {
                ForageBean annotation = provider.type().getAnnotation(ForageBean.class);
                if (annotation != null && annotation.value().equals(trimmedName)) {
                    matched = provider.get();
                    break;
                }
            }
            if (matched == null) {
                throw new RuntimeForageException(
                        "No output guardrail provider found for name '%s'".formatted(trimmedName));
            }
            LOG.info("Creating output guardrail '{}'", trimmedName);
            OutputGuardrail guardrail = matched.create(null);
            if (guardrail != null) {
                guardrails.add(guardrail);
            }
        }
        return guardrails;
    }
}
