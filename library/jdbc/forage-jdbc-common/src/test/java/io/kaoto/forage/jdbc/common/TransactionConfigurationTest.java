package io.kaoto.forage.jdbc.common;

import java.util.ArrayList;
import io.kaoto.forage.jdbc.common.transactions.TransactionConfiguration;
import com.arjuna.ats.jta.common.JTAEnvironmentBean;
import com.arjuna.common.internal.util.propertyservice.BeanPopulator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.parallel.Resources.SYSTEM_PROPERTIES;

@DisplayName("TransactionConfiguration Tests")
@ResourceLock(SYSTEM_PROPERTIES)
class TransactionConfigurationTest {

    @BeforeEach
    void resetNarayanaState() {
        BeanPopulator.getDefaultInstance(JTAEnvironmentBean.class).setXaRecoveryNodes(new ArrayList<>());
    }

    @Test
    @DisplayName("Two datasources with different node IDs both accumulate in recovery nodes")
    void twoDataSourcesAccumulateRecoveryNodes() {
        System.setProperty("forage.jdbc.transaction.node.id", "node-a");

        try {
            DataSourceFactoryConfig configA = new DataSourceFactoryConfig(null);
            new TransactionConfiguration(configA, "ds-a").initializeNarayana();

            System.setProperty("forage.jdbc.transaction.node.id", "node-b");
            DataSourceFactoryConfig configB = new DataSourceFactoryConfig(null);
            new TransactionConfiguration(configB, "ds-b").initializeNarayana();

            JTAEnvironmentBean jtaBean = BeanPopulator.getDefaultInstance(JTAEnvironmentBean.class);
            assertThat(jtaBean.getXaRecoveryNodes()).containsExactlyInAnyOrder("node-a", "node-b");
        } finally {
            System.clearProperty("forage.jdbc.transaction.node.id");
        }
    }

    @Test
    @DisplayName("Same node ID registered twice is not duplicated in recovery nodes")
    void sameNodeIdNotDuplicated() {
        System.setProperty("forage.jdbc.transaction.node.id", "shared-node");

        try {
            DataSourceFactoryConfig configA = new DataSourceFactoryConfig(null);
            new TransactionConfiguration(configA, "ds-a").initializeNarayana();

            DataSourceFactoryConfig configB = new DataSourceFactoryConfig(null);
            new TransactionConfiguration(configB, "ds-b").initializeNarayana();

            JTAEnvironmentBean jtaBean = BeanPopulator.getDefaultInstance(JTAEnvironmentBean.class);
            assertThat(jtaBean.getXaRecoveryNodes()).containsExactly("shared-node");
        } finally {
            System.clearProperty("forage.jdbc.transaction.node.id");
        }
    }
}
