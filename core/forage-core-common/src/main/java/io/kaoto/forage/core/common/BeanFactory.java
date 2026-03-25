package io.kaoto.forage.core.common;

import java.util.List;
import java.util.ServiceLoader;
import org.apache.camel.CamelContextAware;

/**
 * Factory interface for creating and configuring beans within the Forage ecosystem.
 * <p>Implementations of this interface are automatically discovered via ServiceLoader mechanism
 * and are called during Camel context initialization to configure beans.
 */
public interface BeanFactory extends CamelContextAware {

    /**
     * Configures and creates beans for the Camel context.
     * This method is called automatically during Camel context initialization.
     */
    void configure();

    /**
     * Cleans up beans previously created by this factory.
     *
     * <p>Called before {@link #configure()} during hot-reload to allow closing resources
     * (connection pools, JMS connections, etc.) and unbinding old beans from the registry.
     * This ensures that {@code configure()} can re-create and re-bind beans with fresh
     * configuration values.
     *
     * <p>Implementations should:
     * <ul>
     *   <li>Close any {@link AutoCloseable} resources (e.g., DataSource connection pools)</li>
     *   <li>Unbind beans from the Camel registry so they can be re-bound by {@code configure()}</li>
     * </ul>
     *
     * <p>The default implementation is a no-op. Factories that manage stateful resources
     * should override this method.
     */
    default void cleanup() {
        // no-op by default
    }

    /**
     * Utility method to find service providers of a specific type using ServiceLoader.
     *
     * @param <K> the type of service to find
     * @param type the class type to search for
     * @return a list of ServiceLoader providers for the specified type
     */
    default <K> List<ServiceLoader.Provider<K>> findProviders(Class<K> type) {
        ServiceLoader<K> modelLoader =
                ServiceLoader.load(type, getCamelContext().getApplicationContextClassLoader());

        return modelLoader.stream().toList();
    }
}
