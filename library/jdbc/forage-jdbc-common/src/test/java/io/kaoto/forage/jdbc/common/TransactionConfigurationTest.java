package io.kaoto.forage.jdbc.common;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import io.kaoto.forage.jdbc.common.transactions.TransactionConfiguration;
import com.arjuna.ats.arjuna.common.CoreEnvironmentBean;
import com.arjuna.ats.arjuna.common.ObjectStoreEnvironmentBean;
import com.arjuna.ats.jta.common.JTAEnvironmentBean;
import com.arjuna.common.internal.util.propertyservice.BeanPopulator;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.ResourceLock;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.parallel.Resources.SYSTEM_PROPERTIES;

@DisplayName("TransactionConfiguration Tests")
@ResourceLock(SYSTEM_PROPERTIES)
class TransactionConfigurationTest {

    private static final String NODE_ID_PROPERTY = "forage.jdbc.transaction.node.id";
    private static final String OBJECT_STORE_DIR_PROPERTY = "forage.jdbc.transaction.object.store.directory";

    private String originalNodeId;
    private List<String> originalRecoveryNodes;
    private List<String> originalOrphanFilters;
    private String originalDefaultStoreDir;
    private String originalStateStoreDir;
    private String originalCommunicationStoreDir;

    @BeforeEach
    void captureNarayanaState() {
        originalNodeId =
                BeanPopulator.getDefaultInstance(CoreEnvironmentBean.class).getNodeIdentifier();
        JTAEnvironmentBean jtaBean = BeanPopulator.getDefaultInstance(JTAEnvironmentBean.class);
        originalRecoveryNodes = jtaBean.getXaRecoveryNodes();
        originalOrphanFilters = jtaBean.getXaResourceOrphanFilterClassNames();
        originalDefaultStoreDir = BeanPopulator.getDefaultInstance(ObjectStoreEnvironmentBean.class)
                .getObjectStoreDir();
        originalStateStoreDir = BeanPopulator.getNamedInstance(ObjectStoreEnvironmentBean.class, "stateStore")
                .getObjectStoreDir();
        originalCommunicationStoreDir = BeanPopulator.getNamedInstance(
                        ObjectStoreEnvironmentBean.class, "communicationStore")
                .getObjectStoreDir();

        jtaBean.setXaRecoveryNodes(new ArrayList<>());
    }

    @AfterEach
    void restoreNarayanaState() throws Exception {
        System.clearProperty(NODE_ID_PROPERTY);
        System.clearProperty(OBJECT_STORE_DIR_PROPERTY);

        if (originalNodeId != null) {
            BeanPopulator.getDefaultInstance(CoreEnvironmentBean.class).setNodeIdentifier(originalNodeId);
        }
        JTAEnvironmentBean jtaBean = BeanPopulator.getDefaultInstance(JTAEnvironmentBean.class);
        jtaBean.setXaRecoveryNodes(originalRecoveryNodes == null ? new ArrayList<>() : originalRecoveryNodes);
        jtaBean.setXaResourceOrphanFilterClassNames(originalOrphanFilters);
        BeanPopulator.getDefaultInstance(ObjectStoreEnvironmentBean.class).setObjectStoreDir(originalDefaultStoreDir);
        BeanPopulator.getNamedInstance(ObjectStoreEnvironmentBean.class, "stateStore")
                .setObjectStoreDir(originalStateStoreDir);
        BeanPopulator.getNamedInstance(ObjectStoreEnvironmentBean.class, "communicationStore")
                .setObjectStoreDir(originalCommunicationStoreDir);
    }

    @Test
    @DisplayName("Two datasources with different node IDs both accumulate in recovery nodes")
    void twoDataSourcesAccumulateRecoveryNodes() {
        System.setProperty(NODE_ID_PROPERTY, "node-a");

        DataSourceFactoryConfig configA = new DataSourceFactoryConfig(null);
        new TransactionConfiguration(configA, "ds-a").initializeNarayana();

        System.setProperty(NODE_ID_PROPERTY, "node-b");
        DataSourceFactoryConfig configB = new DataSourceFactoryConfig(null);
        new TransactionConfiguration(configB, "ds-b").initializeNarayana();

        JTAEnvironmentBean jtaBean = BeanPopulator.getDefaultInstance(JTAEnvironmentBean.class);
        assertThat(jtaBean.getXaRecoveryNodes()).containsExactlyInAnyOrder("node-a", "node-b");
    }

    @Test
    @DisplayName("Same node ID registered twice is not duplicated in recovery nodes")
    void sameNodeIdNotDuplicated() {
        System.setProperty(NODE_ID_PROPERTY, "shared-node");

        DataSourceFactoryConfig configA = new DataSourceFactoryConfig(null);
        new TransactionConfiguration(configA, "ds-a").initializeNarayana();

        DataSourceFactoryConfig configB = new DataSourceFactoryConfig(null);
        new TransactionConfiguration(configB, "ds-b").initializeNarayana();

        JTAEnvironmentBean jtaBean = BeanPopulator.getDefaultInstance(JTAEnvironmentBean.class);
        assertThat(jtaBean.getXaRecoveryNodes()).containsExactly("shared-node");
    }

    @Test
    @DisplayName("Node identifier is set once: the first configured value wins, later values are ignored")
    void nodeIdentifierSetOnceFirstWins() {
        CoreEnvironmentBean coreBean = BeanPopulator.getDefaultInstance(CoreEnvironmentBean.class);
        String nodeIdBefore = coreBean.getNodeIdentifier();

        System.setProperty(NODE_ID_PROPERTY, "set-once-first");
        new TransactionConfiguration(new DataSourceFactoryConfig(null), "ds-a").initializeNarayana();

        String activeNodeId = coreBean.getNodeIdentifier();
        if (nodeIdBefore == null || "1".equals(nodeIdBefore)) {
            // Node id was unset (Narayana default), so the first initialization claims it
            assertThat(activeNodeId).isEqualTo("set-once-first");
        } else {
            // Node id was already claimed earlier in this JVM, so it must be unchanged
            assertThat(activeNodeId).isEqualTo(nodeIdBefore);
        }

        // A second initialization with a different node id must not change the active one
        System.setProperty(NODE_ID_PROPERTY, "set-once-second");
        new TransactionConfiguration(new DataSourceFactoryConfig(null), "ds-b").initializeNarayana();
        assertThat(coreBean.getNodeIdentifier()).isEqualTo(activeNodeId);
    }

    @Test
    @DisplayName("Configured object store directory is applied to default, stateStore and communicationStore beans")
    void objectStoreDirectoryAppliedToAllStores(@TempDir Path tempDir) {
        String storeDir = tempDir.resolve("narayana-object-store").toString();
        System.setProperty(OBJECT_STORE_DIR_PROPERTY, storeDir);

        new TransactionConfiguration(new DataSourceFactoryConfig(null), "ds-store").initializeNarayana();

        assertThat(BeanPopulator.getDefaultInstance(ObjectStoreEnvironmentBean.class)
                        .getObjectStoreDir())
                .isEqualTo(storeDir);
        assertThat(BeanPopulator.getNamedInstance(ObjectStoreEnvironmentBean.class, "stateStore")
                        .getObjectStoreDir())
                .isEqualTo(storeDir);
        assertThat(BeanPopulator.getNamedInstance(ObjectStoreEnvironmentBean.class, "communicationStore")
                        .getObjectStoreDir())
                .isEqualTo(storeDir);
    }
}
