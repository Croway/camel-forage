package io.kaoto.forage.jms.common.transactions;

import jakarta.jms.ConnectionFactory;

import java.util.Set;
import org.apache.camel.component.jms.JmsComponent;
import org.apache.camel.spi.ComponentCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.jta.JtaTransactionManager;
import com.arjuna.ats.internal.jta.transaction.arjunacore.TransactionSynchronizationRegistryImple;
import com.arjuna.ats.internal.jta.transaction.arjunacore.UserTransactionImple;

/**
 * Wires JTA transaction management into the Camel JMS component when
 * {@code forage.jms.transaction.enabled=true}.
 *
 * <p>Setting a {@link JtaTransactionManager} on the JMS component makes Spring's
 * {@code DefaultMessageListenerContainer} begin a JTA transaction around each
 * {@code receive()}, so XA sessions obtained from the pooled XA connection factory
 * enlist in the Narayana transaction and a rollback returns the message to the broker.
 *
 * <p>Endpoints must NOT enable local JMS transactions ({@code transacted=true}) when XA
 * is active: brokers such as IBM MQ reject locally-transacted work on XA connections
 * that have no coordinating transaction manager (MQRC_SYNCPOINT_NOT_AVAILABLE, 2072).
 * See issue #427.
 */
public final class JmsJtaTransactionSupport {
    private static final Logger LOG = LoggerFactory.getLogger(JmsJtaTransactionSupport.class);

    private JmsJtaTransactionSupport() {}

    /**
     * Creates a Spring {@link JtaTransactionManager} backed by the JVM-global Narayana
     * transaction manager — the same instance the pooled XA connection factory and the
     * JTA transaction policies use, so all participants share one transaction.
     */
    public static JtaTransactionManager createJtaTransactionManager() {
        JtaTransactionManager transactionManager = new JtaTransactionManager(
                new UserTransactionImple(), com.arjuna.ats.jta.TransactionManager.transactionManager());
        transactionManager.setTransactionSynchronizationRegistry(new TransactionSynchronizationRegistryImple());
        transactionManager.afterPropertiesSet();
        return transactionManager;
    }

    /**
     * Creates a {@link ComponentCustomizer} that sets the given transaction manager on every
     * {@link JmsComponent} added to the CamelContext (unless one is already configured).
     * Camel core applies customizers found in the registry on all runtimes.
     */
    public static ComponentCustomizer jmsComponentCustomizer(JtaTransactionManager transactionManager) {
        return ComponentCustomizer.builder(JmsComponent.class).build((name, component) -> {
            if (component.getConfiguration().getTransactionManager() != null) {
                LOG.debug("Camel JMS component '{}' already has a transaction manager, leaving it untouched", name);
                return;
            }
            LOG.info("Configuring Camel JMS component '{}' with the Forage JTA transaction manager", name);
            component.setTransactionManager(transactionManager);

            // JmsComponent.doInit() auto-discovers a single ConnectionFactory from the registry
            // ONLY when no transaction manager is set; setting one above disables that discovery,
            // so replicate it here (endpoints referencing #name explicitly are unaffected)
            if (component.getConfiguration().getConnectionFactory() == null
                    && component.isAllowAutoWiredConnectionFactory()) {
                Set<ConnectionFactory> beans =
                        component.getCamelContext().getRegistry().findByType(ConnectionFactory.class);
                if (beans.size() == 1) {
                    ConnectionFactory connectionFactory = beans.iterator().next();
                    LOG.info("Configuring Camel JMS component '{}' with ConnectionFactory {}", name, connectionFactory);
                    component.setConnectionFactory(connectionFactory);
                } else if (beans.size() > 1) {
                    LOG.debug(
                            "Cannot autowire ConnectionFactory on component '{}' as {} instances found in registry",
                            name,
                            beans.size());
                }
            }
        });
    }
}
