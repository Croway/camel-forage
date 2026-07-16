package io.kaoto.forage.agent;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import org.apache.camel.component.langchain4j.agent.api.Agent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.kaoto.forage.agent.factory.ConfigurationAware;
import io.kaoto.forage.core.ai.ChatMemoryBeanProvider;
import io.kaoto.forage.core.ai.EmbeddingModelAware;
import io.kaoto.forage.core.ai.EmbeddingModelProvider;
import io.kaoto.forage.core.ai.EmbeddingStoreAware;
import io.kaoto.forage.core.ai.EmbeddingStoreProvider;
import io.kaoto.forage.core.ai.MaxMessagesAware;
import io.kaoto.forage.core.ai.ModelProvider;
import io.kaoto.forage.core.ai.RetrievalAugmentorProvider;
import io.kaoto.forage.core.annotations.ForageBean;
import io.kaoto.forage.core.guardrails.InputGuardrailProvider;
import io.kaoto.forage.core.guardrails.OutputGuardrailProvider;
import io.kaoto.forage.core.util.config.ConfigHelper;
import io.kaoto.forage.core.util.config.ConfigStore;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.guardrail.InputGuardrail;
import dev.langchain4j.guardrail.OutputGuardrail;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.store.embedding.EmbeddingStore;

/**
 * Reusable utility for creating Agent instances from configuration.
 *
 * <p>Extracted from {@link AgentBeanFactory} so that both the plain Camel factory,
 * the Quarkus recorder, and the Spring Boot auto-configuration can share the same
 * agent composition logic.
 *
 * <p>Note: the {@code forage.agent.*} to {@code forage.<provider>.*} property bridge (see
 * {@link #getProviderConfigPrefix(String)}) covers common model properties only. Provider-specific
 * required properties — e.g. {@code forage.watsonxai.url} and {@code forage.watsonxai.project.id}
 * for watsonx-ai — must still be set directly and cannot be expressed as {@code forage.agent.*}.
 */
public final class AgentCreator {

    private static final Logger LOG = LoggerFactory.getLogger(AgentCreator.class);
    public static final String DEFAULT_AGENT = "agent";
    private static final String FEATURE_MEMORY = "memory";

    private static final Object CREATION_LOCK = new Object();

    private AgentCreator() {}

    /**
     * Creates an Agent using the full pipeline: ChatModel + Memory + RAG + Guardrails.
     */
    public static Agent createAgent(AgentConfig config, String name, ClassLoader classLoader) {
        String modelKind = config.modelKind();
        if (modelKind == null) {
            LOG.warn("No model kind configured for agent '{}'", name);
            return null;
        }

        LOG.info("Creating agent '{}' with model kind: {}", name, modelKind);

        ChatModel chatModel = createChatModel(config, modelKind, name, classLoader);
        if (chatModel == null) {
            LOG.warn("Failed to create chat model for agent '{}'", name);
            return null;
        }

        return createAgent(config, name, classLoader, chatModel);
    }

    /**
     * Creates an Agent with a pre-created ChatModel (e.g., from Quarkus CDI).
     */
    public static Agent createAgent(AgentConfig config, String name, ClassLoader classLoader, ChatModel chatModel) {
        String modelKind = config.modelKind();
        if (modelKind == null) {
            LOG.warn("No model kind configured for agent '{}'", name);
            return null;
        }

        ChatMemoryProvider chatMemoryProvider = null;
        if (config.hasFeature(FEATURE_MEMORY)) {
            String memoryKind = config.memoryKind();
            if (memoryKind != null) {
                chatMemoryProvider = createMemoryProvider(config, memoryKind, classLoader);
            } else {
                chatMemoryProvider = createDefaultMemoryProvider(config);
            }
        }

        Agent agent = findAndCreateAgent(classLoader);
        if (agent == null) {
            LOG.warn("No Agent implementation found in classpath");
            return null;
        }

        if (agent instanceof ConfigurationAware configurationAware) {
            ForageAgentConfiguration agentConfiguration = new ForageAgentConfiguration();
            agentConfiguration.withChatModel(chatModel).withChatMemoryProvider(chatMemoryProvider);

            // Only create embedding/RAG pipeline when embedding properties are configured
            if (config.hasEmbeddingConfig()) {
                EmbeddingModel embeddingModel = createEmbeddingModel(config, modelKind, name, classLoader);
                if (embeddingModel == null) {
                    throw new IllegalStateException(
                            "Embedding configuration is present for agent '%s' but no EmbeddingModelProvider was found for kind '%s'. "
                                            .formatted(name, modelKind)
                                    + "Add the corresponding forage embedding model dependency to the classpath "
                                    + "(e.g. forage-model-embeddings-ollama, forage-model-embeddings-openai).");
                }

                EmbeddingStore<TextSegment> embeddingStore =
                        createEmbeddingStore(config, modelKind, name, classLoader, embeddingModel);

                RetrievalAugmentor retrievalAugmentor =
                        createRetrievalAugmentor(config, modelKind, name, classLoader, embeddingModel, embeddingStore);

                if (retrievalAugmentor != null) {
                    agentConfiguration.withRetrievalAugmentor(retrievalAugmentor);
                } else {
                    LOG.error(
                            "RAG pipeline could not be assembled for agent '{}': "
                                    + "embedding model was created but retrieval augmentor is null. "
                                    + "Ensure an EmbeddingStoreProvider and RetrievalAugmentorProvider are on the classpath.",
                            name);
                }
            }

            List<InputGuardrail> inputGuardrails = loadInputGuardrails(name, classLoader);
            if (!inputGuardrails.isEmpty()) {
                agentConfiguration.withInputGuardrails(inputGuardrails);
                LOG.info("Configured {} input guardrails for agent '{}'", inputGuardrails.size(), name);
            }

            List<OutputGuardrail> outputGuardrails = loadOutputGuardrails(name, classLoader);
            if (!outputGuardrails.isEmpty()) {
                agentConfiguration.withOutputGuardrails(outputGuardrails);
                LOG.info("Configured {} output guardrails for agent '{}'", outputGuardrails.size(), name);
            }

            configurationAware.configure(agentConfiguration);
        }

        return agent;
    }

    /**
     * Detects agent prefixes from properties.
     */
    public static Set<String> detectPrefixes(ClassLoader classLoader) {
        ConfigStore.getInstance().setClassLoader(classLoader);
        AgentConfig defaultConfig = new AgentConfig();
        return ConfigStore.getInstance().readPrefixes(defaultConfig, ConfigHelper.getNamedPropertyRegexp("agent"));
    }

    /**
     * Checks if default (non-prefixed) agent configuration exists.
     */
    public static boolean hasDefaultConfig(ClassLoader classLoader) {
        ConfigStore.getInstance().setClassLoader(classLoader);
        AgentConfig defaultConfig = new AgentConfig();
        return !ConfigStore.getInstance()
                .readPrefixes(defaultConfig, ConfigHelper.getDefaultPropertyRegexp("agent"))
                .isEmpty();
    }

    static ChatModel createChatModel(AgentConfig config, String modelKind, String agentName, ClassLoader classLoader) {
        List<ServiceLoader.Provider<ModelProvider>> providers = findModelProviders(classLoader);

        for (ServiceLoader.Provider<ModelProvider> provider : providers) {
            Class<? extends ModelProvider> providerClass = provider.type();
            ForageBean annotation = providerClass.getAnnotation(ForageBean.class);
            if (annotation != null && annotation.value().equals(modelKind)) {
                LOG.debug("Found model provider for kind '{}': {}", modelKind, providerClass.getName());
                ModelProvider modelProvider = provider.get();

                String providerPrefix = getProviderConfigPrefix(modelKind);
                String prefix = DEFAULT_AGENT.equals(agentName) ? null : agentName;

                synchronized (CREATION_LOCK) {
                    Map<String, String> previousValues = new LinkedHashMap<>();
                    // The set calls run inside the try so that the finally block restores any
                    // already-set properties even when an argument (e.g. a malformed
                    // forage.agent.temperature) throws mid-sequence.
                    try {
                        setSystemPropertyIfNotNull(previousValues, prefix, providerPrefix, "api.key", config.apiKey());
                        setSystemPropertyIfNotNull(
                                previousValues, prefix, providerPrefix, "model.name", config.modelName());
                        setSystemPropertyIfNotNull(
                                previousValues, prefix, providerPrefix, "base.url", config.baseUrl());
                        setSystemPropertyIfNotNull(
                                previousValues, prefix, providerPrefix, "temperature", config.temperature());
                        setSystemPropertyIfNotNull(
                                previousValues, prefix, providerPrefix, "max.tokens", config.maxTokens());
                        setSystemPropertyIfNotNull(previousValues, prefix, providerPrefix, "top.p", config.topP());
                        setSystemPropertyIfNotNull(previousValues, prefix, providerPrefix, "top.k", config.topK());
                        setSystemPropertyIfNotNull(
                                previousValues, prefix, providerPrefix, "endpoint", config.endpoint());
                        setSystemPropertyIfNotNull(
                                previousValues, prefix, providerPrefix, "deployment.name", config.deploymentName());
                        setSystemPropertyIfNotNull(
                                previousValues, prefix, providerPrefix, "log.requests", config.logRequests());
                        setSystemPropertyIfNotNull(
                                previousValues, prefix, providerPrefix, "log.responses", config.logResponses());
                        setSystemPropertyIfNotNull(previousValues, prefix, providerPrefix, "timeout", config.timeout());

                        return modelProvider.create(prefix);
                    } finally {
                        restoreSystemProperties(previousValues);
                    }
                }
            }
        }

        LOG.warn("No chat model provider found for kind: {}", modelKind);
        return null;
    }

    static EmbeddingModel createEmbeddingModel(
            AgentConfig config, String modelKind, String agentName, ClassLoader classLoader) {
        List<ServiceLoader.Provider<EmbeddingModelProvider>> providers = findEmbeddingModelProviders(classLoader);

        for (ServiceLoader.Provider<EmbeddingModelProvider> provider : providers) {
            Class<? extends EmbeddingModelProvider> providerClass = provider.type();
            ForageBean annotation = providerClass.getAnnotation(ForageBean.class);
            if (annotation != null && annotation.value().equals(modelKind)) {
                LOG.debug("Found embedding model provider for kind '{}': {}", modelKind, providerClass.getName());
                EmbeddingModelProvider modelProvider = provider.get();

                String providerPrefix = getProviderConfigPrefix(modelKind);
                String prefix = DEFAULT_AGENT.equals(agentName) ? null : agentName;

                synchronized (CREATION_LOCK) {
                    Map<String, String> previousValues = new LinkedHashMap<>();
                    // Set calls run inside the try so partially-set properties are always restored
                    // even when an argument (e.g. a malformed embedding.model.timeout) throws.
                    try {
                        setSystemPropertyIfNotNull(
                                previousValues,
                                prefix,
                                providerPrefix,
                                "embedding.model.api.key",
                                config.embeddingModelApiKey());
                        setSystemPropertyIfNotNull(
                                previousValues,
                                prefix,
                                providerPrefix,
                                "embedding.model.name",
                                config.embeddingModelName());
                        setSystemPropertyIfNotNull(
                                previousValues,
                                prefix,
                                providerPrefix,
                                "embedding.model.timeout",
                                config.embeddingModelTimeout());
                        setSystemPropertyIfNotNull(
                                previousValues,
                                prefix,
                                providerPrefix,
                                "embedding.model.max.retries",
                                config.embeddingModelMaxRetries());
                        setSystemPropertyIfNotNull(
                                previousValues,
                                prefix,
                                providerPrefix,
                                "embedding.model.base.url",
                                config.embeddingModelBaseUrl());

                        return modelProvider.create(prefix);
                    } finally {
                        restoreSystemProperties(previousValues);
                    }
                }
            }
        }

        String availableKinds = providers.stream()
                .map(p -> p.type().getAnnotation(ForageBean.class))
                .filter(a -> a != null)
                .map(ForageBean::value)
                .reduce((a, b) -> a + ", " + b)
                .orElse("none");
        LOG.error(
                "No embedding model provider found for kind '{}'. Available embedding providers: {}",
                modelKind,
                availableKinds);
        return null;
    }

    static EmbeddingStore<TextSegment> createEmbeddingStore(
            AgentConfig config, String modelKind, String agentName, ClassLoader classLoader, EmbeddingModel model) {
        List<ServiceLoader.Provider<EmbeddingStoreProvider>> providers = findEmbeddingStoreProviders(classLoader);

        if (!providers.isEmpty()) {
            ServiceLoader.Provider<EmbeddingStoreProvider> provider = providers.get(0);
            Class<? extends EmbeddingStoreProvider> providerClass = provider.type();
            LOG.debug("Found embedding store provider for kind '{}': {}", modelKind, providerClass.getName());
            EmbeddingStoreProvider storeProvider = provider.get();

            String prefix = DEFAULT_AGENT.equals(agentName) ? null : agentName;

            if (storeProvider instanceof EmbeddingModelAware modelAware) {
                modelAware.withEmbeddingModel(model);
            }

            synchronized (CREATION_LOCK) {
                Map<String, String> previousValues = new LinkedHashMap<>();
                // Set calls run inside the try so partially-set properties are always restored
                // even when an argument (e.g. a malformed max.size) throws.
                try {
                    setSystemPropertyIfNotNull(
                            previousValues, prefix, "in.memory.store", "file.source", config.fileSource());
                    setSystemPropertyIfNotNull(
                            previousValues, prefix, "in.memory.store", "max.size", config.embeddingStoreMaxSize());
                    setSystemPropertyIfNotNull(
                            previousValues,
                            prefix,
                            "in.memory.store",
                            "overlap.size",
                            config.embeddingStoreOverlapSize());

                    return storeProvider.create(prefix);
                } finally {
                    restoreSystemProperties(previousValues);
                }
            }
        }

        LOG.error("No embedding store provider found on the classpath for agent '{}'", agentName);
        return null;
    }

    static RetrievalAugmentor createRetrievalAugmentor(
            AgentConfig config,
            String modelKind,
            String agentName,
            ClassLoader classLoader,
            EmbeddingModel embeddingModel,
            EmbeddingStore<TextSegment> embeddingStore) {
        List<ServiceLoader.Provider<RetrievalAugmentorProvider>> providers =
                findRetrievalAugmentorProviders(classLoader);

        if (!providers.isEmpty()) {
            ServiceLoader.Provider<RetrievalAugmentorProvider> provider = providers.get(0);
            Class<? extends RetrievalAugmentorProvider> providerClass = provider.type();
            LOG.debug("Found retrieval augmentor provider for kind '{}': {}", modelKind, providerClass.getName());
            RetrievalAugmentorProvider retrievalAugmentorProvider = provider.get();

            String prefix = DEFAULT_AGENT.equals(agentName) ? null : agentName;

            if (retrievalAugmentorProvider instanceof EmbeddingModelAware modelAware) {
                modelAware.withEmbeddingModel(embeddingModel);
            }
            if (retrievalAugmentorProvider instanceof EmbeddingStoreAware storeAware) {
                storeAware.withEmbeddingStore(embeddingStore);
            }

            synchronized (CREATION_LOCK) {
                Map<String, String> previousValues = new LinkedHashMap<>();
                // Set calls run inside the try so partially-set properties are always restored
                // even when an argument (e.g. a malformed min.score) throws.
                try {
                    setSystemPropertyIfNotNull(
                            previousValues, prefix, "rag", "max.results", config.defaultRagMaxResults());
                    setSystemPropertyIfNotNull(previousValues, prefix, "rag", "min.score", config.defaultRagMinScore());

                    return retrievalAugmentorProvider.create(prefix);
                } finally {
                    restoreSystemProperties(previousValues);
                }
            }
        }

        LOG.error("No retrieval augmentor provider found on the classpath for agent '{}'", agentName);
        return null;
    }

    static List<InputGuardrail> loadInputGuardrails(String agentName, ClassLoader classLoader) {
        List<InputGuardrail> guardrails = new ArrayList<>();
        ServiceLoader<InputGuardrailProvider> loader = ServiceLoader.load(InputGuardrailProvider.class, classLoader);
        List<ServiceLoader.Provider<InputGuardrailProvider>> providers =
                loader.stream().toList();

        LOG.info("Found {} input guardrail providers for agent '{}'", providers.size(), agentName);

        for (ServiceLoader.Provider<InputGuardrailProvider> provider : providers) {
            try {
                InputGuardrailProvider guardrailProvider = provider.get();
                String prefix = DEFAULT_AGENT.equals(agentName) ? null : agentName;
                LOG.info(
                        "Creating guardrail from provider {} with prefix '{}'",
                        provider.type().getName(),
                        prefix);
                InputGuardrail guardrail = guardrailProvider.create(prefix);
                if (guardrail != null) {
                    guardrails.add(guardrail);
                    LOG.info("Loaded input guardrail: {}", guardrail.getClass().getName());
                }
            } catch (Exception e) {
                LOG.warn(
                        "Skipping input guardrail provider {}: {}",
                        provider.type().getName(),
                        e.getMessage());
            }
        }

        LOG.info("Total input guardrails loaded: {}", guardrails.size());
        return guardrails;
    }

    static List<OutputGuardrail> loadOutputGuardrails(String agentName, ClassLoader classLoader) {
        List<OutputGuardrail> guardrails = new ArrayList<>();
        ServiceLoader<OutputGuardrailProvider> loader = ServiceLoader.load(OutputGuardrailProvider.class, classLoader);
        List<ServiceLoader.Provider<OutputGuardrailProvider>> providers =
                loader.stream().toList();

        for (ServiceLoader.Provider<OutputGuardrailProvider> provider : providers) {
            try {
                OutputGuardrailProvider guardrailProvider = provider.get();
                String prefix = DEFAULT_AGENT.equals(agentName) ? null : agentName;
                OutputGuardrail guardrail = guardrailProvider.create(prefix);
                if (guardrail != null) {
                    guardrails.add(guardrail);
                    LOG.debug(
                            "Loaded output guardrail: {}", guardrail.getClass().getName());
                }
            } catch (Exception e) {
                LOG.debug(
                        "Skipping output guardrail provider {}: {}",
                        provider.type().getName(),
                        e.getMessage());
            }
        }

        return guardrails;
    }

    static Agent findAndCreateAgent(ClassLoader classLoader) {
        ServiceLoader<Agent> loader = ServiceLoader.load(Agent.class, classLoader);
        List<ServiceLoader.Provider<Agent>> providers = loader.stream().toList();
        if (!providers.isEmpty()) {
            return providers.get(0).get();
        }
        return null;
    }

    static ChatMemoryProvider createMemoryProvider(AgentConfig config, String memoryKind, ClassLoader classLoader) {
        ServiceLoader<ChatMemoryBeanProvider> loader = ServiceLoader.load(ChatMemoryBeanProvider.class, classLoader);
        List<ServiceLoader.Provider<ChatMemoryBeanProvider>> providers =
                loader.stream().toList();

        for (ServiceLoader.Provider<ChatMemoryBeanProvider> provider : providers) {
            Class<? extends ChatMemoryBeanProvider> providerClass = provider.type();
            ForageBean annotation = providerClass.getAnnotation(ForageBean.class);
            if (annotation != null && annotation.value().equals(memoryKind)) {
                LOG.debug("Found memory provider for kind '{}': {}", memoryKind, providerClass.getName());
                ChatMemoryBeanProvider memoryProvider = provider.get();
                if (memoryProvider instanceof MaxMessagesAware maxMessagesAware) {
                    maxMessagesAware.withMaxMessages(config.memoryMaxMessages());
                }
                return memoryProvider.create();
            }
        }

        LOG.warn("No memory provider found for kind '{}', using default", memoryKind);
        return createDefaultMemoryProvider(config);
    }

    static ChatMemoryProvider createDefaultMemoryProvider(AgentConfig config) {
        int maxMessages = config.memoryMaxMessages();
        return memoryId -> MessageWindowChatMemory.builder()
                .id(memoryId)
                .maxMessages(maxMessages)
                .build();
    }

    static void setSystemPropertyIfNotNull(
            Map<String, String> previousValues, String prefix, String providerPrefix, String key, Object value) {
        if (value != null) {
            String fullKey = prefix != null
                    ? "forage." + prefix + "." + providerPrefix + "." + key
                    : "forage." + providerPrefix + "." + key;
            previousValues.put(fullKey, System.getProperty(fullKey));
            System.setProperty(fullKey, String.valueOf(value));
            LOG.trace("Set system property: {}={}", fullKey, value);
        }
    }

    static void restoreSystemProperties(Map<String, String> previousValues) {
        for (Map.Entry<String, String> entry : previousValues.entrySet()) {
            if (entry.getValue() == null) {
                System.clearProperty(entry.getKey());
            } else {
                System.setProperty(entry.getKey(), entry.getValue());
            }
            LOG.trace("Restored system property: {}", entry.getKey());
        }
    }

    /**
     * Maps a model kind (the value of {@code forage.agent.model.kind}) to the configuration
     * prefix used by the corresponding provider module's ConfigEntries class, so that
     * {@code forage.agent.*} properties can be bridged to {@code forage.<prefix>.*} ones.
     *
     * <p><strong>Limitation:</strong> only the common properties handled by the bridge sites in
     * this class are mapped. Provider-specific required properties have no {@code forage.agent.*}
     * counterpart and must be set directly. Notably, watsonx-ai still requires
     * {@code forage.watsonxai.url} and {@code forage.watsonxai.project.id} to be configured
     * directly — they cannot be bridged from {@code forage.agent.*} configuration.
     *
     * <p>Package-private (rather than private) for testability.
     */
    static String getProviderConfigPrefix(String modelKind) {
        return switch (modelKind) {
            case "google-gemini" -> "google";
            case "azure-openai" -> "azure.openai";
            case "openai" -> "openai";
            case "ollama" -> "ollama";
            case "anthropic" -> "anthropic";
            case "mistral-ai" -> "mistralai";
            case "hugging-face" -> "huggingface";
            case "watsonx-ai" -> "watsonxai";
            case "local-ai" -> "localai";
            case "dashscope" -> "dashscope";
            default -> modelKind.replace("-", ".");
        };
    }

    private static List<ServiceLoader.Provider<ModelProvider>> findModelProviders(ClassLoader classLoader) {
        ServiceLoader<ModelProvider> loader = ServiceLoader.load(ModelProvider.class, classLoader);
        return loader.stream().toList();
    }

    private static List<ServiceLoader.Provider<EmbeddingModelProvider>> findEmbeddingModelProviders(
            ClassLoader classLoader) {
        ServiceLoader<EmbeddingModelProvider> loader = ServiceLoader.load(EmbeddingModelProvider.class, classLoader);
        return loader.stream().toList();
    }

    private static List<ServiceLoader.Provider<EmbeddingStoreProvider>> findEmbeddingStoreProviders(
            ClassLoader classLoader) {
        ServiceLoader<EmbeddingStoreProvider> loader = ServiceLoader.load(EmbeddingStoreProvider.class, classLoader);
        return loader.stream().toList();
    }

    private static List<ServiceLoader.Provider<RetrievalAugmentorProvider>> findRetrievalAugmentorProviders(
            ClassLoader classLoader) {
        ServiceLoader<RetrievalAugmentorProvider> loader =
                ServiceLoader.load(RetrievalAugmentorProvider.class, classLoader);
        return loader.stream().toList();
    }
}
