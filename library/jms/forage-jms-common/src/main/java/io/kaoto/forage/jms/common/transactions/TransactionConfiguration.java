package io.kaoto.forage.jms.common.transactions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.kaoto.forage.jms.common.ConnectionFactoryConfig;
import com.arjuna.ats.arjuna.common.CoordinatorEnvironmentBean;
import com.arjuna.ats.arjuna.common.CoreEnvironmentBeanException;
import com.arjuna.ats.arjuna.common.ObjectStoreEnvironmentBean;
import com.arjuna.ats.arjuna.common.RecoveryEnvironmentBean;
import com.arjuna.ats.internal.arjuna.objectstore.VolatileStore;
import com.arjuna.ats.jta.common.JTAEnvironmentBean;
import com.arjuna.ats.jta.resources.LastResourceCommitOptimisation;
import com.arjuna.common.internal.util.propertyservice.BeanPopulator;

/**
 * Manages Narayana transaction manager configuration for JMS operations.
 * Handles initialization of XA transaction support and recovery mechanisms.
 */
public class TransactionConfiguration {
    private static final Logger LOG = LoggerFactory.getLogger(TransactionConfiguration.class);

    private final ConnectionFactoryConfig config;
    private final String instanceId;

    public TransactionConfiguration(ConnectionFactoryConfig config, String instanceId) {
        this.config = config;
        this.instanceId = instanceId;
    }

    public void initializeNarayana() {
        LOG.info("Initializing Narayana transaction manager for instance: {}", instanceId);

        try {
            configureNodeIdentifier();
            configureObjectStore();
            configureCoordinator();
            configureRecovery();
            configureJTA();
            LOG.info("Narayana transaction manager initialized successfully");
        } catch (Exception e) {
            LOG.error("Failed to initialize Narayana transaction manager", e);
            throw new RuntimeException("Transaction manager initialization failed", e);
        }
    }

    private void configureNodeIdentifier() throws CoreEnvironmentBeanException {
        var coreEnvironmentBean = com.arjuna.ats.arjuna.common.arjPropertyManager.getCoreEnvironmentBean();

        String nodeId = config.transactionNodeId();
        if (nodeId != null) {
            LOG.debug("Setting transaction node identifier: {}", nodeId);
            coreEnvironmentBean.setNodeIdentifier(nodeId);
        } else {
            LOG.debug("Using default node identifier for instance: {}", instanceId);
        }
    }

    private void configureObjectStore() {
        String objectStoreType = config.transactionObjectStoreType();
        String objectStoreDir = config.transactionObjectStoreDirectory();

        LOG.debug("Configuring object store - Type: {}, Directory: {}", objectStoreType, objectStoreDir);

        if ("volatile".equalsIgnoreCase(objectStoreType)) {
            for (String storeName : new String[] {null, "stateStore", "communicationStore"}) {
                ObjectStoreEnvironmentBean bean = storeName == null
                        ? BeanPopulator.getDefaultInstance(ObjectStoreEnvironmentBean.class)
                        : BeanPopulator.getNamedInstance(ObjectStoreEnvironmentBean.class, storeName);
                bean.setObjectStoreType(VolatileStore.class.getName());
            }
        } else {
            BeanPopulator.getDefaultInstance(ObjectStoreEnvironmentBean.class).setObjectStoreDir(objectStoreDir);
            BeanPopulator.getNamedInstance(ObjectStoreEnvironmentBean.class, "stateStore")
                    .setObjectStoreDir(objectStoreDir);
            BeanPopulator.getNamedInstance(ObjectStoreEnvironmentBean.class, "communicationStore")
                    .setObjectStoreDir(objectStoreDir);
        }
    }

    private void configureCoordinator() {
        LOG.debug("Configuring coordinator with timeout: {} seconds", config.transactionTimeoutSeconds());
        BeanPopulator.getDefaultInstance(CoordinatorEnvironmentBean.class)
                .setDefaultTimeout(config.transactionTimeoutSeconds());
    }

    private void configureRecovery() {
        if (!config.transactionEnableRecovery()) {
            LOG.debug("Transaction recovery is disabled");
            return;
        }

        LOG.debug("Configuring transaction recovery mechanisms");

        RecoveryEnvironmentBean recoveryEnvironmentBean =
                com.arjuna.ats.arjuna.common.recoveryPropertyManager.getRecoveryEnvironmentBean();

        recoveryEnvironmentBean.setRecoveryModuleClassNames(
                Arrays.stream(config.transactionRecoveryModules().split(","))
                        .map(String::trim)
                        .toList());

        recoveryEnvironmentBean.setExpiryScannerClassNames(
                Arrays.stream(config.transactionExpiryScanners().split(","))
                        .map(String::trim)
                        .toList());

        LOG.info("Transaction recovery configured with modules: {}", config.transactionRecoveryModules());
    }

    private void configureJTA() {
        JTAEnvironmentBean jtaBean = BeanPopulator.getDefaultInstance(JTAEnvironmentBean.class);

        // Accumulate recovery nodes instead of replacing
        String nodeId = config.transactionNodeId() != null ? config.transactionNodeId() : instanceId;
        List<String> currentNodes = jtaBean.getXaRecoveryNodes();
        if (currentNodes == null || !currentNodes.contains(nodeId)) {
            List<String> updatedNodes = new ArrayList<>(currentNodes == null ? List.of() : currentNodes);
            updatedNodes.add(nodeId);
            jtaBean.setXaRecoveryNodes(updatedNodes);
        }

        jtaBean.setLastResourceOptimisationInterface(LastResourceCommitOptimisation.class);

        jtaBean.setXaResourceOrphanFilterClassNames(
                Arrays.stream(config.transactionXaResourceOrphanFilters().split(","))
                        .map(String::trim)
                        .toList());

        LOG.debug("JTA configured with recovery node: {}", nodeId);
    }
}
