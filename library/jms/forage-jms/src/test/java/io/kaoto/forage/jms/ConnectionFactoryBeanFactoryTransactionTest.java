package io.kaoto.forage.jms;

import jakarta.jms.Connection;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSContext;

import java.util.List;
import org.apache.camel.CamelContext;
import org.apache.camel.impl.DefaultCamelContext;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that JTA transaction policy beans are bound when only a prefixed (named)
 * JMS configuration enables transactions (issue #230). The prefixed configuration
 * ({@code forage.mq1.jms.transaction.enabled=true}) comes from the test classpath's
 * {@code forage-connectionfactory.properties}; the default (unprefixed) configuration
 * has transactions disabled.
 */
class ConnectionFactoryBeanFactoryTransactionTest {

    private static final List<String> POLICY_BEANS =
            List.of("PROPAGATION_REQUIRED", "MANDATORY", "NEVER", "NOT_SUPPORTED", "REQUIRES_NEW", "SUPPORTS");

    @Test
    void prefixedTransactionEnabledBindsJtaPolicies() {
        CamelContext camelContext = new DefaultCamelContext();
        // Pre-bind the prefixed ConnectionFactory so configure() does not need a broker provider
        camelContext.getRegistry().bind("mq1", dummyConnectionFactory());

        ConnectionFactoryBeanFactory beanFactory = new ConnectionFactoryBeanFactory();
        beanFactory.setCamelContext(camelContext);
        beanFactory.configure();

        for (String name : POLICY_BEANS) {
            assertThat(camelContext.getRegistry().lookupByName(name))
                    .as("JTA policy bean '%s' should be bound when a prefixed config enables transactions", name)
                    .isNotNull();
        }
    }

    private static ConnectionFactory dummyConnectionFactory() {
        return new ConnectionFactory() {
            @Override
            public Connection createConnection() {
                throw new UnsupportedOperationException("test stub");
            }

            @Override
            public Connection createConnection(String userName, String password) {
                throw new UnsupportedOperationException("test stub");
            }

            @Override
            public JMSContext createContext() {
                throw new UnsupportedOperationException("test stub");
            }

            @Override
            public JMSContext createContext(String userName, String password) {
                throw new UnsupportedOperationException("test stub");
            }

            @Override
            public JMSContext createContext(String userName, String password, int sessionMode) {
                throw new UnsupportedOperationException("test stub");
            }

            @Override
            public JMSContext createContext(int sessionMode) {
                throw new UnsupportedOperationException("test stub");
            }
        };
    }
}
