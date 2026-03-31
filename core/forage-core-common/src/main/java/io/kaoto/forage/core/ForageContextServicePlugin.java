package io.kaoto.forage.core;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import org.apache.camel.CamelContext;
import org.apache.camel.spi.CamelEvent;
import org.apache.camel.spi.ContextServicePlugin;
import org.apache.camel.support.EventNotifierSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.kaoto.forage.core.common.BeanFactory;

public class ForageContextServicePlugin implements ContextServicePlugin {
    private static final Logger LOG = LoggerFactory.getLogger(ForageContextServicePlugin.class);

    @Override
    public void load(CamelContext camelContext) {
        List<BeanFactory> factories = new ArrayList<>();

        ServiceLoader<BeanFactory> loader =
                ServiceLoader.load(BeanFactory.class, camelContext.getApplicationContextClassLoader());

        loader.forEach(beanFactory -> {
            try {
                beanFactory.setCamelContext(camelContext);
                beanFactory.configure();
                factories.add(beanFactory);
                LOG.debug(
                        "Successfully configured bean factory: {}",
                        beanFactory.getClass().getName());
            } catch (Exception e) {
                LOG.warn(
                        "Failed to configure bean factory: {}",
                        beanFactory.getClass().getName(),
                        e);
            }
        });

        // Defer hot-reload watcher startup to after the CamelContext is fully started.
        // At load() time, camel.main.routesReloadEnabled is not yet set on the
        // PropertiesComponent (the dev profile is configured after build()), so
        // isReloadEnabled() would return false. By waiting for CamelContextStartedEvent,
        // all initial properties are guaranteed to be available.
        if (!factories.isEmpty()) {
            camelContext.getManagementStrategy().addEventNotifier(new EventNotifierSupport() {
                @Override
                public void notify(CamelEvent event) throws Exception {
                    if (event instanceof CamelEvent.CamelContextStartedEvent) {
                        if (isReloadEnabled(camelContext)) {
                            try {
                                ForageReloadWatcher watcher = new ForageReloadWatcher(factories);
                                camelContext.addService(watcher);
                                LOG.info("Forage hot-reload enabled: watching for property file changes");
                            } catch (Exception e) {
                                LOG.warn("Failed to start Forage hot-reload watcher", e);
                            }
                        }
                    }
                }
            });
        }
    }

    /**
     * Checks whether hot-reload should be enabled.
     *
     * <p>Reload is enabled when either:
     * <ul>
     *   <li>The system property {@code forage.reload.enabled} is set to {@code true}</li>
     *   <li>The Camel property {@code camel.main.routesReloadEnabled} is {@code true}
     *       (set by the {@code --dev} flag in Camel JBang)</li>
     * </ul>
     */
    private boolean isReloadEnabled(CamelContext camelContext) {
        if ("true".equals(System.getProperty("forage.reload.enabled"))) {
            return true;
        }
        return camelContext
                .getPropertiesComponent()
                .resolveProperty("camel.main.routesReloadEnabled")
                .filter("true"::equals)
                .isPresent();
    }
}
