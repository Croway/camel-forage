package io.kaoto.forage.jdbc.common;

import javax.sql.DataSource;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import io.kaoto.forage.core.util.config.ConfigStore;
import io.kaoto.forage.core.util.config.MissingConfigException;
import io.kaoto.forage.jdbc.common.transactions.TransactionConfiguration;
import com.arjuna.ats.arjuna.common.CoreEnvironmentBean;
import com.arjuna.ats.arjuna.common.ObjectStoreEnvironmentBean;
import com.arjuna.ats.internal.arjuna.objectstore.jdbc.JDBCStore;
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

@DisplayName("TransactionConfiguration Tests")
@ResourceLock(SYSTEM_PROPERTIES)
class TransactionConfigurationTest {

    private static final String NODE_ID_PROPERTY = "forage.jdbc.transaction.node.id";
    private static final String OBJECT_STORE_DIR_PROPERTY = "forage.jdbc.transaction.object.store.directory";
    private static final String OBJECT_STORE_TYPE_PROPERTY = "forage.jdbc.transaction.object.store.type";
    private static final String OBJECT_STORE_DATASOURCE_PROPERTY = "forage.jdbc.transaction.object.store.datasource";

    private String originalNodeId;
    private List<String> originalRecoveryNodes;
    private List<String> originalOrphanFilters;
    private String originalDefaultStoreDir;
    private String originalStateStoreDir;
    private String originalCommunicationStoreDir;
    private String originalDefaultStoreType;
    private DataSource originalDefaultJdbcDs;
    private DataSource originalStateJdbcDs;
    private DataSource originalCommJdbcDs;

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
        originalDefaultStoreType = BeanPopulator.getDefaultInstance(ObjectStoreEnvironmentBean.class)
                .getObjectStoreType();
        originalDefaultJdbcDs = BeanPopulator.getDefaultInstance(ObjectStoreEnvironmentBean.class)
                .getJdbcDataSource();
        originalStateJdbcDs = BeanPopulator.getNamedInstance(ObjectStoreEnvironmentBean.class, "stateStore")
                .getJdbcDataSource();
        originalCommJdbcDs = BeanPopulator.getNamedInstance(ObjectStoreEnvironmentBean.class, "communicationStore")
                .getJdbcDataSource();

        jtaBean.setXaRecoveryNodes(new ArrayList<>());
    }

    @AfterEach
    void restoreNarayanaState() throws Exception {
        System.clearProperty(NODE_ID_PROPERTY);
        System.clearProperty(OBJECT_STORE_DIR_PROPERTY);
        System.clearProperty(OBJECT_STORE_TYPE_PROPERTY);
        System.clearProperty(OBJECT_STORE_DATASOURCE_PROPERTY);
        System.clearProperty("forage.txlog.jdbc.url");
        System.clearProperty("forage.txlog.jdbc.username");
        System.clearProperty("forage.txlog.jdbc.password");
        System.clearProperty("forage.txlog.jdbc.db.kind");
        System.clearProperty("forage.txlog.jdbc.transaction.enabled");

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
        if (originalDefaultStoreType != null) {
            BeanPopulator.getDefaultInstance(ObjectStoreEnvironmentBean.class)
                    .setObjectStoreType(originalDefaultStoreType);
        }
        BeanPopulator.getDefaultInstance(ObjectStoreEnvironmentBean.class).setJdbcDataSource(originalDefaultJdbcDs);
        BeanPopulator.getNamedInstance(ObjectStoreEnvironmentBean.class, "stateStore")
                .setJdbcDataSource(originalStateJdbcDs);
        BeanPopulator.getNamedInstance(ObjectStoreEnvironmentBean.class, "communicationStore")
                .setJdbcDataSource(originalCommJdbcDs);
        ConfigStore.getInstance().reload();
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

    @Test
    @DisplayName("JDBC object store configures JDBCStore type and datasource on all three beans")
    void jdbcObjectStoreConfigured() {
        System.setProperty(OBJECT_STORE_TYPE_PROPERTY, "jdbc");
        System.setProperty(OBJECT_STORE_DATASOURCE_PROPERTY, "txlog");
        System.setProperty("forage.txlog.jdbc.url", "jdbc:h2:mem:objstore;DB_CLOSE_DELAY=-1");
        System.setProperty("forage.txlog.jdbc.username", "sa");
        System.setProperty("forage.txlog.jdbc.password", "");
        System.setProperty("forage.txlog.jdbc.db.kind", "h2");

        new TransactionConfiguration(new DataSourceFactoryConfig(null), "ds-jdbc").initializeNarayana();

        String expectedType = JDBCStore.class.getName();
        for (String storeName : new String[] {null, "stateStore", "communicationStore"}) {
            ObjectStoreEnvironmentBean bean = storeName == null
                    ? BeanPopulator.getDefaultInstance(ObjectStoreEnvironmentBean.class)
                    : BeanPopulator.getNamedInstance(ObjectStoreEnvironmentBean.class, storeName);
            assertThat(bean.getObjectStoreType()).isEqualTo(expectedType);
            assertThat(bean.getJdbcDataSource()).isNotNull();
        }
    }

    @Test
    @DisplayName("JDBC object store without datasource reference throws MissingConfigException")
    void jdbcObjectStoreWithoutDatasourceThrows() {
        System.setProperty(OBJECT_STORE_TYPE_PROPERTY, "jdbc");

        assertThatThrownBy(() -> new TransactionConfiguration(new DataSourceFactoryConfig(null), "ds-no-ds")
                        .initializeNarayana())
                .isInstanceOf(MissingConfigException.class)
                .hasMessageContaining("object.store.datasource");
    }

    @Test
    @DisplayName("JDBC object store rejects XA-enabled datasource")
    void jdbcObjectStoreRejectsXaDatasource() {
        System.setProperty(OBJECT_STORE_TYPE_PROPERTY, "jdbc");
        System.setProperty(OBJECT_STORE_DATASOURCE_PROPERTY, "txlog");
        System.setProperty("forage.txlog.jdbc.url", "jdbc:h2:mem:xa;DB_CLOSE_DELAY=-1");
        System.setProperty("forage.txlog.jdbc.username", "sa");
        System.setProperty("forage.txlog.jdbc.password", "");
        System.setProperty("forage.txlog.jdbc.db.kind", "h2");
        System.setProperty("forage.txlog.jdbc.transaction.enabled", "true");

        assertThatThrownBy(() ->
                        new TransactionConfiguration(new DataSourceFactoryConfig(null), "ds-xa").initializeNarayana())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("non-XA");
    }
}
