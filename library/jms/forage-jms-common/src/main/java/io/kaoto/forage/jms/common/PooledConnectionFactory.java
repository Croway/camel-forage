package io.kaoto.forage.jms.common;

import jakarta.jms.ConnectionFactory;
import jakarta.jms.XAConnectionFactory;

import org.messaginghub.pooled.jms.JmsPoolConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.kaoto.forage.core.jms.ConnectionFactoryProvider;
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
            new TransactionConfiguration(config, id == null ? "connectionFactory" : id).initializeNarayana();
            XAConnectionFactory xaConnectionFactory = createXAConnectionFactory(config);

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

            // The XAConnectionFactory is wrapped as a plain ConnectionFactory, so sessions do NOT
            // enlist in JTA transactions. Enlisting via JmsPoolXAConnectionFactory alone breaks
            // consumption on brokers that reject local transactions on XA connections (IBM MQ
            // MQRC 2072): completing XA also needs a JtaTransactionManager wired into the Camel
            // JMS component. Tracked in #427.
            final JmsPoolConnectionFactory pooledConnectionFactory = setupPooledConnectionFactory(xaConnectionFactory);

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

    protected ConnectionFactoryConfig getConfig() {
        return config;
    }
}
