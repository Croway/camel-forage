package io.kaoto.forage.jms.common;

import jakarta.jms.Connection;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSContext;
import jakarta.jms.XAConnection;
import jakarta.jms.XAConnectionFactory;
import jakarta.jms.XAJMSContext;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.messaginghub.pooled.jms.JmsPoolXAConnectionFactory;
import io.kaoto.forage.core.jta.recovery.ForageRecoveryService;
import io.kaoto.forage.core.util.config.ConfigStore;
import com.arjuna.ats.arjuna.common.CoreEnvironmentBean;
import com.arjuna.ats.jta.common.JTAEnvironmentBean;
import com.arjuna.common.internal.util.propertyservice.BeanPopulator;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.ResourceLock;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.parallel.Resources.SYSTEM_PROPERTIES;

@DisplayName("PooledConnectionFactory XA Tests")
@ResourceLock(SYSTEM_PROPERTIES)
class PooledConnectionFactoryTest {

    private static final String TRANSACTION_ENABLED_PROPERTY = "forage.jms.transaction.enabled";
    private static final String POOL_ENABLED_PROPERTY = "forage.jms.pool.enabled";
    private static final String ENABLE_RECOVERY_PROPERTY = "forage.jms.transaction.enable.recovery";
    private static final String OBJECT_STORE_DIRECTORY_PROPERTY = "forage.jms.transaction.object.store.directory";
    private static final String PERIODIC_RECOVERY_THREAD = "Periodic Recovery";

    @TempDir
    Path objectStoreDir;

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
        ForageRecoveryService.getInstance().shutdown();
        System.clearProperty(TRANSACTION_ENABLED_PROPERTY);
        System.clearProperty(POOL_ENABLED_PROPERTY);
        System.clearProperty(ENABLE_RECOVERY_PROPERTY);
        System.clearProperty(OBJECT_STORE_DIRECTORY_PROPERTY);
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
    @DisplayName("XA branch returns a pooled XA factory enlisting sessions with the Narayana TM (#427)")
    void xaBranchReturnsPooledXaFactoryWithTransactionManager() {
        System.setProperty(TRANSACTION_ENABLED_PROPERTY, "true");

        ConnectionFactory connectionFactory =
                new StubPooledConnectionFactory(new StubDualConnectionFactory()).create(null);

        assertThat(connectionFactory).isInstanceOf(JmsPoolXAConnectionFactory.class);
        JmsPoolXAConnectionFactory pooled = (JmsPoolXAConnectionFactory) connectionFactory;
        assertThat(pooled.getTransactionManager())
                .as("XA pool must enlist sessions with the JVM-global Narayana transaction manager")
                .isSameAs(com.arjuna.ats.jta.TransactionManager.transactionManager());
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

    @Test
    @DisplayName("Recovery disabled (default): no recovery manager thread is started (#432)")
    void recoveryDisabledDoesNotStartRecoveryManager() {
        System.setProperty(TRANSACTION_ENABLED_PROPERTY, "true");

        new StubPooledConnectionFactory(new StubDualConnectionFactory()).create(null);

        assertThat(periodicRecoveryThreadRunning())
                .as("no Periodic Recovery thread may run when enable.recovery=false")
                .isFalse();
    }

    @Test
    @DisplayName("Recovery enabled: helper is registered and the recovery manager runs (#432)")
    void recoveryEnabledRegistersHelperAndStartsManager() {
        System.setProperty(TRANSACTION_ENABLED_PROPERTY, "true");
        System.setProperty(ENABLE_RECOVERY_PROPERTY, "true");
        System.setProperty(
                OBJECT_STORE_DIRECTORY_PROPERTY, objectStoreDir.toAbsolutePath().toString());

        new StubPooledConnectionFactory(new StubDualConnectionFactory()).create(null);

        assertThat(periodicRecoveryThreadRunning())
                .as("the recovery manager must run once a helper is registered")
                .isTrue();

        // the helper is tracked under the default instance key: deregistering it lets the manager stop
        ForageRecoveryService.getInstance().deregisterHelpers(PooledConnectionFactory.recoveryKey("connectionFactory"));
        ForageRecoveryService.getInstance().stopIfNoRegistrations();
        awaitNoPeriodicRecoveryThread();
        assertThat(periodicRecoveryThreadRunning()).isFalse();
    }

    private static boolean periodicRecoveryThreadRunning() {
        return Thread.getAllStackTraces().keySet().stream()
                .anyMatch(thread -> thread.getName().contains(PERIODIC_RECOVERY_THREAD) && thread.isAlive());
    }

    private static void awaitNoPeriodicRecoveryThread() {
        long deadline = System.currentTimeMillis() + 10_000;
        while (periodicRecoveryThreadRunning() && System.currentTimeMillis() < deadline) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
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
