package io.kaoto.forage.core.jta;

import javax.sql.DataSource;

import java.sql.SQLException;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.agroal.api.AgroalDataSource;
import io.agroal.api.configuration.supplier.AgroalDataSourceConfigurationSupplier;
import io.agroal.api.security.NamePrincipal;
import io.agroal.api.security.SimplePassword;
import io.agroal.api.transaction.TransactionIntegration;
import com.arjuna.ats.arjuna.common.ObjectStoreEnvironmentBean;
import com.arjuna.ats.internal.arjuna.objectstore.jdbc.JDBCStore;
import com.arjuna.common.internal.util.propertyservice.BeanPopulator;

public final class ObjectStoreJdbcSupport {

    private static final Logger LOG = LoggerFactory.getLogger(ObjectStoreJdbcSupport.class);

    private ObjectStoreJdbcSupport() {}

    public static AgroalDataSource createObjectStoreDataSource(String jdbcUrl, String username, String password) {
        AgroalDataSourceConfigurationSupplier configSupplier = new AgroalDataSourceConfigurationSupplier();

        configSupplier
                .connectionPoolConfiguration()
                .connectionFactoryConfiguration()
                .jdbcUrl(jdbcUrl)
                .principal(new NamePrincipal(username))
                .credential(new SimplePassword(password));

        configSupplier
                .connectionPoolConfiguration()
                .initialSize(1)
                .minSize(1)
                .maxSize(5)
                .acquisitionTimeout(Duration.ofSeconds(5))
                .transactionIntegration(TransactionIntegration.none());

        try {
            return AgroalDataSource.from(configSupplier.get());
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create object store DataSource", e);
        }
    }

    public static boolean configureJdbcObjectStore(
            DataSource dataSource, boolean createTable, boolean dropTable, String tablePrefix) {
        ObjectStoreEnvironmentBean defaultBean = BeanPopulator.getDefaultInstance(ObjectStoreEnvironmentBean.class);

        synchronized (defaultBean) {
            if (defaultBean.getJdbcDataSource() != null) {
                LOG.debug("JDBC object store already configured by another module, skipping");
                return false;
            }

            String storeType = JDBCStore.class.getName();
            for (String storeName : new String[] {null, "stateStore", "communicationStore"}) {
                ObjectStoreEnvironmentBean bean = storeName == null
                        ? defaultBean
                        : BeanPopulator.getNamedInstance(ObjectStoreEnvironmentBean.class, storeName);
                bean.setObjectStoreType(storeType);
                bean.setJdbcDataSource(dataSource);
                bean.setCreateTable(createTable);
                bean.setDropTable(dropTable);
                bean.setTablePrefix(tablePrefix);
            }

            LOG.info(
                    "JDBC object store configured (createTable={}, dropTable={}, tablePrefix={})",
                    createTable,
                    dropTable,
                    tablePrefix);
            return true;
        }
    }
}
