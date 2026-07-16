package io.kaoto.forage.quarkus.jms.deployment;

import jakarta.jms.ConnectionFactory;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.camel.quarkus.core.deployment.spi.CamelRuntimeBeanBuildItem;
import org.apache.camel.quarkus.core.deployment.spi.RuntimeCamelContextCustomizerBuildItem;
import org.apache.camel.spi.CamelContextCustomizer;
import org.apache.camel.spi.ComponentCustomizer;
import org.jboss.logging.Logger;
import io.kaoto.forage.core.annotations.FactoryType;
import io.kaoto.forage.core.annotations.FactoryVariant;
import io.kaoto.forage.core.annotations.ForageFactory;
import io.kaoto.forage.core.common.ForageQuarkusConfigSourceAdapter;
import io.kaoto.forage.core.util.config.ConfigHelper;
import io.kaoto.forage.core.util.config.ConfigStore;
import io.kaoto.forage.jms.common.ConnectionFactoryConfig;
import io.kaoto.forage.jms.common.JmsModuleDescriptor;
import io.kaoto.forage.quarkus.jms.ForageJmsRecorder;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.runtime.RuntimeValue;

@ForageFactory(
        value = "JMS Connection (Quarkus)",
        components = {"camel-jms"},
        description = "Native JMS ConnectionFactory for Quarkus with compile-time optimization",
        type = FactoryType.CONNECTION_FACTORY,
        autowired = true,
        configClass = ConnectionFactoryConfig.class,
        variant = FactoryVariant.QUARKUS,
        runtimeDependencies = {"mvn:org.apache.camel.quarkus:camel-quarkus-jms"})
public class ForageJmsProcessor {

    private static final Logger LOG = Logger.getLogger(ForageJmsProcessor.class);
    private static final String FEATURE = "forage-jms";
    private static final JmsModuleDescriptor DESCRIPTOR = new JmsModuleDescriptor();

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    @Record(value = ExecutionTime.RUNTIME_INIT)
    void registerIbmMqConnectionFactory(ForageJmsRecorder recorder, BuildProducer<CamelRuntimeBeanBuildItem> beans) {

        Map<String, ConnectionFactoryConfig> configs = discoverConfigs();
        if (configs.isEmpty()) {
            LOG.debug("No Forage JMS configuration found, skipping ConnectionFactory discovery");
            return;
        }

        for (Map.Entry<String, ConnectionFactoryConfig> entry : configs.entrySet()) {
            if ("ibmmq".equals(entry.getValue().jmsKind())) {
                LOG.info("Recording IBM MQ connection factory for url: "
                        + entry.getValue().brokerUrl());
                String beanName = Optional.ofNullable(entry.getKey()).orElse(DESCRIPTOR.defaultBeanName());
                RuntimeValue<ConnectionFactory> connectionFactory =
                        recorder.createIbmMQConnectionFactory(entry.getKey());
                beans.produce(
                        new CamelRuntimeBeanBuildItem(beanName, ConnectionFactory.class.getName(), connectionFactory));
            }
        }
    }

    /**
     * Wires a JTA transaction manager into the default Camel JMS component when any Forage
     * JMS configuration enables transactions (#427). Named components are pre-configured
     * with per-broker JTA scoping (#433).
     */
    @BuildStep
    @Record(value = ExecutionTime.RUNTIME_INIT)
    void registerJmsTransactionManagerCustomizer(
            ForageJmsRecorder recorder, BuildProducer<CamelRuntimeBeanBuildItem> beans) {

        boolean anyTransactionEnabled =
                discoverConfigs().values().stream().anyMatch(ConnectionFactoryConfig::transactionEnabled);

        if (anyTransactionEnabled) {
            beans.produce(new CamelRuntimeBeanBuildItem(
                    "forageJmsTransactionManagerCustomizer",
                    ComponentCustomizer.class.getName(),
                    recorder.createJmsTransactionManagerCustomizer()));
        }
    }

    /**
     * Registers per-broker {@code JmsComponent} instances so routes using
     * {@code mq1:queue:...} get the correct ConnectionFactory and JTA scoping (#433).
     */
    @BuildStep
    @Record(value = ExecutionTime.RUNTIME_INIT)
    void registerPerBrokerJmsComponents(
            ForageJmsRecorder recorder, BuildProducer<RuntimeCamelContextCustomizerBuildItem> customizers) {

        Map<String, ConnectionFactoryConfig> configs = discoverConfigs();
        Map<String, Boolean> namedPrefixes = configs.entrySet().stream()
                .filter(e -> e.getKey() != null)
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().transactionEnabled()));

        if (!namedPrefixes.isEmpty()) {
            RuntimeValue<CamelContextCustomizer> customizer =
                    recorder.createPerBrokerJmsComponentCustomizer(namedPrefixes);
            customizers.produce(new RuntimeCamelContextCustomizerBuildItem(customizer));
        }
    }

    private Map<String, ConnectionFactoryConfig> discoverConfigs() {
        ConnectionFactoryConfig defaultConfig = DESCRIPTOR.createConfig(null);
        Set<String> named = ConfigStore.getInstance()
                .readPrefixes(defaultConfig, ConfigHelper.getNamedPropertyRegexp(DESCRIPTOR.modulePrefix()));
        if (named.isEmpty()) {
            named = ForageQuarkusConfigSourceAdapter.getDiscoveredPrefixes(DESCRIPTOR.modulePrefix());
        }

        if (!named.isEmpty()) {
            return named.stream().collect(Collectors.toMap(n -> n, DESCRIPTOR::createConfig));
        }

        // Check if default (unprefixed) properties exist before creating a default config
        Set<String> defaultPrefixes = ConfigStore.getInstance()
                .readPrefixes(defaultConfig, ConfigHelper.getDefaultPropertyRegexp(DESCRIPTOR.modulePrefix()));
        if (!defaultPrefixes.isEmpty()) {
            return Collections.singletonMap(null, defaultConfig);
        }
        return Collections.emptyMap();
    }
}
