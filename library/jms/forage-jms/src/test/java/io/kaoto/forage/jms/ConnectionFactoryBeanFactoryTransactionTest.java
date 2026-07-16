package io.kaoto.forage.jms;

import jakarta.jms.Connection;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSContext;

import java.util.List;
import org.apache.camel.CamelContext;
import org.apache.camel.component.jms.JmsComponent;
import org.apache.camel.impl.DefaultCamelContext;
import org.springframework.transaction.jta.JtaTransactionManager;

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
        // Pre-bind the prefixed ConnectionFactories so configure() does not need a broker provider
        camelContext.getRegistry().bind("mq1", dummyConnectionFactory());
        camelContext.getRegistry().bind("mq2", dummyConnectionFactory());

        ConnectionFactoryBeanFactory beanFactory = new ConnectionFactoryBeanFactory();
        beanFactory.setCamelContext(camelContext);
        beanFactory.configure();

        for (String name : POLICY_BEANS) {
            assertThat(camelContext.getRegistry().lookupByName(name))
                    .as("JTA policy bean '%s' should be bound when a prefixed config enables transactions", name)
                    .isNotNull();
        }
    }

    @Test
    void transactionEnabledWiresJtaTransactionManagerIntoNamedComponent() {
        CamelContext camelContext = new DefaultCamelContext();
        ConnectionFactory connectionFactory = dummyConnectionFactory();
        camelContext.getRegistry().bind("mq1", connectionFactory);
        camelContext.getRegistry().bind("mq2", dummyConnectionFactory());

        ConnectionFactoryBeanFactory beanFactory = new ConnectionFactoryBeanFactory();
        beanFactory.setCamelContext(camelContext);
        beanFactory.configure();

        JtaTransactionManager jtaTransactionManager =
                camelContext.getRegistry().lookupByNameAndType("jtaTransactionManager", JtaTransactionManager.class);
        assertThat(jtaTransactionManager)
                .as("A JtaTransactionManager should be bound when transactions are enabled")
                .isNotNull();

        // The named component (mq1) should have the JTA transaction manager set
        // directly at creation time (#427, #433)
        JmsComponent mq1Component = (JmsComponent) camelContext.getComponent("mq1");
        assertThat(mq1Component.getConfiguration().getTransactionManager())
                .as("Named JmsComponent 'mq1' should receive the Forage JTA transaction manager (#427)")
                .isSameAs(jtaTransactionManager);
        assertThat(mq1Component.getConfiguration().getConnectionFactory())
                .as("Named JmsComponent 'mq1' should use its own ConnectionFactory")
                .isSameAs(connectionFactory);
    }

    @Test
    void perBrokerJmsComponentHasScopedTransactionManager() {
        CamelContext camelContext = new DefaultCamelContext();
        ConnectionFactory mq1Cf = dummyConnectionFactory();
        ConnectionFactory mq2Cf = dummyConnectionFactory();
        camelContext.getRegistry().bind("mq1", mq1Cf);
        camelContext.getRegistry().bind("mq2", mq2Cf);

        ConnectionFactoryBeanFactory beanFactory = new ConnectionFactoryBeanFactory();
        beanFactory.setCamelContext(camelContext);
        beanFactory.configure();

        JtaTransactionManager jtaTransactionManager =
                camelContext.getRegistry().lookupByNameAndType("jtaTransactionManager", JtaTransactionManager.class);

        // mq1 has transactions enabled — its JmsComponent should have a TM
        JmsComponent mq1Component = (JmsComponent) camelContext.getComponent("mq1");
        assertThat(mq1Component)
                .as("A JmsComponent should be registered for prefix 'mq1'")
                .isNotNull();
        assertThat(mq1Component.getConfiguration().getTransactionManager())
                .as("mq1 JmsComponent should have the JTA transaction manager (transactions enabled)")
                .isSameAs(jtaTransactionManager);
        assertThat(mq1Component.getConfiguration().getConnectionFactory())
                .as("mq1 JmsComponent should use the mq1 ConnectionFactory")
                .isSameAs(mq1Cf);

        // mq2 has transactions disabled — its JmsComponent should NOT have a TM
        JmsComponent mq2Component = (JmsComponent) camelContext.getComponent("mq2");
        assertThat(mq2Component)
                .as("A JmsComponent should be registered for prefix 'mq2'")
                .isNotNull();
        assertThat(mq2Component.getConfiguration().getTransactionManager())
                .as("mq2 JmsComponent should NOT have a transaction manager (transactions disabled, #433)")
                .isNull();
        assertThat(mq2Component.getConfiguration().getConnectionFactory())
                .as("mq2 JmsComponent should use the mq2 ConnectionFactory")
                .isSameAs(mq2Cf);
    }

    @Test
    void defaultJmsComponentNotAffectedByNamedPrefixTransactions() {
        CamelContext camelContext = new DefaultCamelContext();
        camelContext.getRegistry().bind("mq1", dummyConnectionFactory());
        camelContext.getRegistry().bind("mq2", dummyConnectionFactory());

        ConnectionFactoryBeanFactory beanFactory = new ConnectionFactoryBeanFactory();
        beanFactory.setCamelContext(camelContext);
        beanFactory.configure();

        // Default (unnamed) config does NOT have transactions enabled, so the default
        // "jms" component should NOT receive the JtaTransactionManager
        JmsComponent jmsComponent = camelContext.getComponent("jms", JmsComponent.class);
        assertThat(jmsComponent.getConfiguration().getTransactionManager())
                .as("Default 'jms' component should NOT have TM when only a named prefix enables transactions (#433)")
                .isNull();
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
