package io.kaoto.forage.jms.common;

import jakarta.jms.Connection;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSContext;
import jakarta.jms.XAConnection;
import jakarta.jms.XAConnectionFactory;
import jakarta.jms.XAJMSContext;

import java.util.ArrayList;
import java.util.List;
import org.messaginghub.pooled.jms.JmsPoolConnectionFactory;
import org.messaginghub.pooled.jms.JmsPoolXAConnectionFactory;
import io.kaoto.forage.core.util.config.ConfigStore;
import com.arjuna.ats.arjuna.common.CoreEnvironmentBean;
import com.arjuna.ats.jta.common.JTAEnvironmentBean;
import com.arjuna.common.internal.util.propertyservice.BeanPopulator;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.parallel.Resources.SYSTEM_PROPERTIES;

@DisplayName("PooledConnectionFactory XA Tests")
@ResourceLock(SYSTEM_PROPERTIES)
class PooledConnectionFactoryTest {

    private static final String TRANSACTION_ENABLED_PROPERTY = "forage.jms.transaction.enabled";
    private static final String POOL_ENABLED_PROPERTY = "forage.jms.pool.enabled";

    private String originalNodeId;
    private List<String> originalRecoveryNodes;

    @BeforeEach
    void captureNarayanaState() {
        originalNodeId =
                BeanPopulator.getDefaultInstance(CoreEnvironmentBean.class).getNodeIdentifier();
        originalRecoveryNodes =
                BeanPopulator.getDefaultInstance(JTAEnvironmentBean.class).getXaRecoveryNodes();
    }

    @AfterEach
    void restoreState() throws Exception {
        System.clearProperty(TRANSACTION_ENABLED_PROPERTY);
        System.clearProperty(POOL_ENABLED_PROPERTY);
        // Constructing the config caches the system-property values in the singleton
        // ConfigStore; clearing the properties alone would leak transaction.enabled=true
        // into later tests in the same JVM
        ConfigStore.getInstance().reload();

        if (originalNodeId != null) {
            BeanPopulator.getDefaultInstance(CoreEnvironmentBean.class).setNodeIdentifier(originalNodeId);
        }
        BeanPopulator.getDefaultInstance(JTAEnvironmentBean.class)
                .setXaRecoveryNodes(originalRecoveryNodes == null ? new ArrayList<>() : originalRecoveryNodes);
    }

    @Test
    @DisplayName("XA branch returns a plain pooled factory: JTA enlistment is not wired yet (#427)")
    void xaBranchReturnsPlainPooledFactory() {
        System.setProperty(TRANSACTION_ENABLED_PROPERTY, "true");

        ConnectionFactory connectionFactory =
                new StubPooledConnectionFactory(new StubDualConnectionFactory()).create(null);

        // A JmsPoolXAConnectionFactory alone breaks consumption on brokers that reject
        // local transactions on XA connections (IBM MQ MQRC 2072); switching to it must
        // come together with Camel JMS component JTA wiring — tracked in #427
        assertThat(connectionFactory).isInstanceOf(JmsPoolConnectionFactory.class);
        assertThat(connectionFactory).isNotInstanceOf(JmsPoolXAConnectionFactory.class);
    }

    @Test
    @DisplayName("XA with pooling disabled returns the raw factory when it also implements ConnectionFactory")
    void xaWithPoolingDisabledReturnsRawFactory() {
        System.setProperty(TRANSACTION_ENABLED_PROPERTY, "true");
        System.setProperty(POOL_ENABLED_PROPERTY, "false");

        StubDualConnectionFactory rawFactory = new StubDualConnectionFactory();
        ConnectionFactory connectionFactory = new StubPooledConnectionFactory(rawFactory).create(null);

        assertThat(connectionFactory).isSameAs(rawFactory);
    }

    @Test
    @DisplayName("XA with pooling disabled throws a descriptive error for XA-only factories")
    void xaWithPoolingDisabledThrowsForXaOnlyFactory() {
        System.setProperty(TRANSACTION_ENABLED_PROPERTY, "true");
        System.setProperty(POOL_ENABLED_PROPERTY, "false");

        StubPooledConnectionFactory provider = new StubPooledConnectionFactory(new StubXAConnectionFactory());

        assertThatThrownBy(() -> provider.create(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("does not implement jakarta.jms.ConnectionFactory")
                .hasMessageContaining("forage.jms.pool.enabled");
    }

    /** Concrete PooledConnectionFactory returning stub factories, no broker required. */
    private static final class StubPooledConnectionFactory extends PooledConnectionFactory {

        private final XAConnectionFactory xaConnectionFactory;

        private StubPooledConnectionFactory(XAConnectionFactory xaConnectionFactory) {
            this.xaConnectionFactory = xaConnectionFactory;
        }

        @Override
        protected ConnectionFactory createConnectionFactory(ConnectionFactoryConfig config) {
            throw new UnsupportedOperationException("not used in XA tests");
        }

        @Override
        protected XAConnectionFactory createXAConnectionFactory(ConnectionFactoryConfig config) {
            return xaConnectionFactory;
        }
    }

    /** XA-only stub: implements XAConnectionFactory but NOT ConnectionFactory. */
    private static class StubXAConnectionFactory implements XAConnectionFactory {

        @Override
        public XAConnection createXAConnection() {
            return null;
        }

        @Override
        public XAConnection createXAConnection(String userName, String password) {
            return null;
        }

        @Override
        public XAJMSContext createXAContext() {
            return null;
        }

        @Override
        public XAJMSContext createXAContext(String userName, String password) {
            return null;
        }
    }

    /** Stub implementing both XAConnectionFactory and ConnectionFactory. */
    private static final class StubDualConnectionFactory extends StubXAConnectionFactory implements ConnectionFactory {

        @Override
        public Connection createConnection() {
            return null;
        }

        @Override
        public Connection createConnection(String userName, String password) {
            return null;
        }

        @Override
        public JMSContext createContext() {
            return null;
        }

        @Override
        public JMSContext createContext(String userName, String password) {
            return null;
        }

        @Override
        public JMSContext createContext(String userName, String password, int sessionMode) {
            return null;
        }

        @Override
        public JMSContext createContext(int sessionMode) {
            return null;
        }
    }
}
