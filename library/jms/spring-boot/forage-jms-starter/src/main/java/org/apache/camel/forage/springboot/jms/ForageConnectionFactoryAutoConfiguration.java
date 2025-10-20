package org.apache.camel.forage.springboot.jms;

import jakarta.annotation.PostConstruct;
import jakarta.jms.ConnectionFactory;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;
import org.apache.camel.forage.core.annotations.ForageFactory;
import org.apache.camel.forage.core.common.ServiceLoaderHelper;
import org.apache.camel.forage.core.jms.ConnectionFactoryProvider;
import org.apache.camel.forage.core.util.config.ConfigStore;
import org.apache.camel.forage.jms.common.ConnectionFactoryCommonExportHelper;
import org.apache.camel.forage.jms.common.ConnectionFactoryConfig;
import org.apache.camel.forage.jms.common.ForageConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@ForageFactory(
        value = "CamelSpringBootConnectionFactoryFactory",
        components = {"camel-jms"},
        description = "Default Spring Boot ConnectionFactory factory with ServiceLoader discovery",
        factoryType = "ConnectionFactory",
        autowired = true)
@Configuration
public class ForageConnectionFactoryAutoConfiguration implements BeanFactoryAware {

    private static final Logger log = LoggerFactory.getLogger(ForageConnectionFactoryAutoConfiguration.class);

    @Configuration
    @ConditionalOnProperty(value = "jms.transaction.enabled", havingValue = "true")
    @EnableTransactionManagement
    class ForageTransactionManagement {

        @PostConstruct
        public void init() {
            log.info("ForageTransactionManagement configuration enabled");
        }
    }

    private BeanFactory beanFactory;

    @PostConstruct
    public void createJmsBeans() {
        log.info("Initializing Forage ConnectionFactory auto-configuration");
        ConfigurableBeanFactory configurableBeanFactory = (ConfigurableBeanFactory) beanFactory;

        ConnectionFactoryConfig config = new ConnectionFactoryConfig();
        Set<String> prefixes = ConfigStore.getInstance().readPrefixes(config, "(.+).jms\\..*");
        log.debug("Found {} prefixes for JMS configuration: {}", prefixes.size(), prefixes);

        if (!prefixes.isEmpty()) {
            log.info("Creating named ConnectionFactory beans for prefixes: {}", prefixes);
            boolean isConnectionFactoryCreated = false;
            for (String name : prefixes) {
                if (!configurableBeanFactory.containsBean(name)) {
                    log.debug("Creating ConnectionFactory bean with name: {}", name);
                    ConnectionFactoryConfig cfConfig = new ConnectionFactoryConfig(name);
                    ForageConnectionFactory forageConnectionFactory = newConnectionFactory(cfConfig, name);
                    configurableBeanFactory.registerSingleton(name, forageConnectionFactory.connectionFactory());
                    log.info("Registered ConnectionFactory bean: {}", name);
                    if (!isConnectionFactoryCreated) {
                        configurableBeanFactory.registerSingleton(
                                "connectionFactory", forageConnectionFactory.connectionFactory());
                        log.info("Registered default ConnectionFactory bean using: {}", name);
                        isConnectionFactoryCreated = true;
                    }
                } else {
                    log.debug("ConnectionFactory bean {} already exists, skipping creation", name);
                }
            }
        } else {
            log.debug("No prefixed JMS configurations found, looking for single ConnectionFactory provider");
            final List<ServiceLoader.Provider<ConnectionFactoryProvider>> providers = findConnectionFactoryProviders();
            if (providers.size() == 1) {
                log.info(
                        "Creating default ConnectionFactory using single provider: {}",
                        providers.get(0).type().getName());
                ForageConnectionFactory forageConnectionFactory = doCreateConnectionFactory(providers.get(0), null);

                configurableBeanFactory.registerSingleton(
                        "jmsConnectionFactory", forageConnectionFactory.connectionFactory());
                log.info("Registered default ConnectionFactory bean");
            } else {
                log.error(
                        "Expected exactly 1 ConnectionFactory provider, but found {}: {}",
                        providers.size(),
                        providers.stream().map(p -> p.type().getName()).toList());
                throw new IllegalArgumentException("No ConnectionFactory implementation is present in the classpath");
            }
        }
    }

    private synchronized ForageConnectionFactory newConnectionFactory(
            ConnectionFactoryConfig connectionFactoryConfig, String name) {
        log.debug(
                "Creating new ConnectionFactory for name: {} with jmsKind: {}",
                name,
                connectionFactoryConfig.jmsKind());
        final String connectionFactoryProviderClass =
                ConnectionFactoryCommonExportHelper.transformJmsKindIntoProviderClass(
                        connectionFactoryConfig.jmsKind());
        log.debug("Resolved provider class: {}", connectionFactoryProviderClass);

        final List<ServiceLoader.Provider<ConnectionFactoryProvider>> providers = findConnectionFactoryProviders();
        log.debug("Found {} ConnectionFactory providers", providers.size());

        ServiceLoader.Provider<ConnectionFactoryProvider> connectionFactoryProvider;
        if (providers.size() == 1) {
            connectionFactoryProvider = providers.get(0);
            log.debug(
                    "Using single available provider: {}",
                    connectionFactoryProvider.type().getName());
        } else {
            connectionFactoryProvider =
                    ServiceLoaderHelper.findProviderByClassName(providers, connectionFactoryProviderClass);
            log.debug(
                    "Selected provider by class name: {}",
                    connectionFactoryProvider != null
                            ? connectionFactoryProvider.type().getName()
                            : "null");
        }

        if (connectionFactoryProvider == null) {
            log.error("No ConnectionFactory provider found for class: {}", connectionFactoryProviderClass);
            return null;
        }

        return doCreateConnectionFactory(connectionFactoryProvider, name);
    }

    private ForageConnectionFactory doCreateConnectionFactory(
            ServiceLoader.Provider<ConnectionFactoryProvider> provider, String name) {
        log.debug(
                "Creating ConnectionFactory instance using provider: {} for name: {}",
                provider.type().getName(),
                name);
        final ConnectionFactoryProvider connectionFactoryProvider = provider.get();
        ConnectionFactory connectionFactory = connectionFactoryProvider.create(name);
        log.debug("Successfully created ConnectionFactory instance for: {}", name);

        return new ForageConnectionFactory(connectionFactory);
    }

    private List<ServiceLoader.Provider<ConnectionFactoryProvider>> findConnectionFactoryProviders() {
        ServiceLoader<ConnectionFactoryProvider> serviceLoader = ServiceLoader.load(
                ConnectionFactoryProvider.class, beanFactory.getClass().getClassLoader());

        List<ServiceLoader.Provider<ConnectionFactoryProvider>> providers =
                serviceLoader.stream().toList();
        log.debug(
                "Found {} ConnectionFactory providers: {}",
                providers.size(),
                providers.stream().map(p -> p.type().getName()).toList());
        return providers;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }
}
