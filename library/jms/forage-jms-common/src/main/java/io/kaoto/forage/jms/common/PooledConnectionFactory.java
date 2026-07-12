package io.kaoto.forage.jms.common;

import jakarta.jms.ConnectionFactory;
import jakarta.jms.XAConnectionFactory;
import jakarta.transaction.TransactionManager;

import org.jboss.narayana.jta.jms.JmsXAResourceRecoveryHelper;
import org.messaginghub.pooled.jms.JmsPoolConnectionFactory;
import org.messaginghub.pooled.jms.JmsPoolXAConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.kaoto.forage.core.jms.ConnectionFactoryProvider;
import io.kaoto.forage.core.jta.recovery.ForageRecoveryService;
import io.kaoto.forage.jms.common.transactions.TransactionConfiguration;

/**
 * Abstract base class for pooled JMS implementations using pooled-jms connection pooling.
 * Provides broker-agnostic ConnectionFactory configuration with optimized pool settings.
 */
public abstract class PooledConnectionFactory implements ConnectionFactoryProvider {
    private static final Logger LOG = LoggerFactory.getLogger(PooledConnectionFactory.class);

    private ConnectionFactoryConfig config;

    /**
     * Creates the underlying ConnectionFactory for the specific JMS provider.
     *
     * @param config the connection factory configuration
     * @return the underlying ConnectionFactory
     */
    protected abstract ConnectionFactory createConnectionFactory(ConnectionFactoryConfig config);

    /**
     * Creates the underlying XAConnectionFactory for the specific JMS provider when transactions are enabled.
     *
     * @param config the connection factory configuration
     * @return the underlying XAConnectionFactory
     */
    protected abstract XAConnectionFactory createXAConnectionFactory(ConnectionFactoryConfig config);

    @Override
    public ConnectionFactory create(String id) {
        LOG.info("Creating ConnectionFactory with id: {}", id);
        return createPooledConnectionFactory(id);
    }

    /**
     * Creates a pooled ConnectionFactory with the given configuration.
     *
     * @param id the configuration ID for logging
     * @return configured pooled ConnectionFactory
     * @throws RuntimeException if ConnectionFactory creation fails
     */
    protected ConnectionFactory createPooledConnectionFactory(String id) {
        config = new ConnectionFactoryConfig(id);

        LOG.info(
                "ConnectionFactory configuration - Broker URL: {}, SSL Enabled: {}, Pool Enabled: {}, Max Connections: {}, "
                        + "Max Sessions Per Connection: {}, Idle Timeout: {}ms, Connection Timeout: {}ms, Transaction Enabled: {}",
                config.brokerUrlOrNull(),
                config.sslEnabled(),
                config.poolEnabled(),
                config.maxConnections(),
                config.maxSessionsPerConnection(),
                config.idleTimeoutMillis(),
                config.connectionTimeoutMillis(),
                config.transactionEnabled());

        if (config.transactionEnabled()) {
            LOG.info("Creating XA ConnectionFactory for transactional support");
            String instanceId = id == null ? "connectionFactory" : id;
            new TransactionConfiguration(config, instanceId).initializeNarayana();
            XAConnectionFactory xaConnectionFactory = createXAConnectionFactory(config);

            if (config.transactionEnableRecovery()) {
                // Recovery needs fresh XA connections to the broker after a crash, obtained
                // from the raw (unpooled) XAConnectionFactory. See issue #432.
                ForageRecoveryService.getInstance()
                        .registerHelper(
                                recoveryKey(instanceId),
                                new JmsXAResourceRecoveryHelper(
                                        xaConnectionFactory, config.username(), config.password()));
            }

            if (!config.poolEnabled()) {
                LOG.info("Connection pooling is disabled, returning underlying XAConnectionFactory");
                if (!(xaConnectionFactory instanceof ConnectionFactory connectionFactory)) {
                    throw new IllegalStateException("XAConnectionFactory of type "
                            + xaConnectionFactory.getClass().getName()
                            + " does not implement jakarta.jms.ConnectionFactory and cannot be returned when "
                            + "connection pooling is disabled (forage.jms.pool.enabled=false).");
                }
                return connectionFactory;
            }

            // Sessions created within an active JTA transaction enlist their XAResource in the
            // Narayana transaction. This alone breaks consumption on brokers that reject local
            // transactions on XA connections (IBM MQ MQRC 2072), so each runtime also wires a
            // JtaTransactionManager into the Camel JMS component (see JmsJtaTransactionSupport)
            // and endpoints must NOT enable local transactions (transacted=true). See #427.
            TransactionManager transactionManager = com.arjuna.ats.jta.TransactionManager.transactionManager();
            final JmsPoolXAConnectionFactory pooledConnectionFactory =
                    setupPooledXAConnectionFactory(xaConnectionFactory, transactionManager);

            LOG.info("Pooled XA ConnectionFactory initialized successfully for id: {}", id);
            return pooledConnectionFactory;
        } else {
            ConnectionFactory underlyingConnectionFactory = createConnectionFactory(config);

            if (!config.poolEnabled()) {
                LOG.info("Connection pooling is disabled, returning underlying ConnectionFactory");
                return underlyingConnectionFactory;
            }

            final JmsPoolConnectionFactory pooledConnectionFactory =
                    setupPooledConnectionFactory(underlyingConnectionFactory);

            LOG.info("Pooled ConnectionFactory initialized successfully for id: {}", id);
            return pooledConnectionFactory;
        }
    }

    private JmsPoolXAConnectionFactory setupPooledXAConnectionFactory(
            XAConnectionFactory xaConnectionFactory, TransactionManager transactionManager) {
        JmsPoolXAConnectionFactory pooledConnectionFactory = new JmsPoolXAConnectionFactory();
        pooledConnectionFactory.setConnectionFactory(xaConnectionFactory);
        pooledConnectionFactory.setTransactionManager(transactionManager);
        applyPoolSettings(pooledConnectionFactory);
        return pooledConnectionFactory;
    }

    private <T> JmsPoolConnectionFactory setupPooledConnectionFactory(T underlyingConnectionFactory) {
        JmsPoolConnectionFactory pooledConnectionFactory = new JmsPoolConnectionFactory();
        pooledConnectionFactory.setConnectionFactory(underlyingConnectionFactory);
        applyPoolSettings(pooledConnectionFactory);
        return pooledConnectionFactory;
    }

    private void applyPoolSettings(JmsPoolConnectionFactory pooledConnectionFactory) {
        pooledConnectionFactory.setMaxConnections(config.maxConnections());
        pooledConnectionFactory.setMaxSessionsPerConnection(config.maxSessionsPerConnection());
        pooledConnectionFactory.setConnectionIdleTimeout((int) config.idleTimeoutMillis());
        pooledConnectionFactory.setConnectionCheckInterval(config.expiryTimeoutMillis());
        pooledConnectionFactory.setBlockIfSessionPoolIsFull(config.blockIfFull());

        if (config.blockIfFull() && config.blockIfFullTimeoutMillis() > 0) {
            pooledConnectionFactory.setBlockIfSessionPoolIsFullTimeout(config.blockIfFullTimeoutMillis());
        }
    }

    /**
     * Key under which a broker instance's XA recovery helper is registered with
     * {@link ForageRecoveryService}. Used by the bean factories to deregister on reload/stop.
     */
    public static String recoveryKey(String instanceId) {
        return "jms:" + instanceId;
    }

    protected ConnectionFactoryConfig getConfig() {
        return config;
    }
}
