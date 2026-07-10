package io.kaoto.forage.quarkus.jdbc;

import javax.sql.DataSource;

import org.apache.camel.CamelContext;
import org.apache.camel.processor.aggregate.jdbc.JdbcAggregationRepository;
import org.apache.camel.processor.idempotent.jdbc.JdbcMessageIdRepository;
import io.kaoto.forage.jdbc.common.DataSourceFactoryConfig;
import io.kaoto.forage.jdbc.common.aggregation.ForageAggregationRepository;
import io.kaoto.forage.jdbc.common.idempotent.ForageIdRepository;
import io.kaoto.forage.jdbc.common.idempotent.ForageJdbcMessageIdRepository;
import io.kaoto.forage.jdbc.db2.Db2Jdbc;
import io.kaoto.forage.jdbc.h2.H2Jdbc;
import io.kaoto.forage.jdbc.hsqldb.HsqldbJdbc;
import io.kaoto.forage.jdbc.mariadb.MariadbJdbc;
import io.kaoto.forage.jdbc.mssql.MssqlJdbc;
import io.kaoto.forage.jdbc.mysql.MysqlJdbc;
import io.kaoto.forage.jdbc.oracle.OracleJdbc;
import io.kaoto.forage.jdbc.postgresql.PostgresqlJdbc;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;

/**
 * Aggregation repository is created via Recorder
 */
@Recorder
public class ForageJdbcRecorder {

    public RuntimeValue<JdbcAggregationRepository> createAggregationRepository(
            String dsName, String prefix, RuntimeValue<CamelContext> camelContext) {

        DataSourceFactoryConfig config = new DataSourceFactoryConfig(prefix);
        CamelContext context = camelContext.getValue();
        DataSource agroalDataSource = context.getRegistry().lookupByNameAndType(dsName, DataSource.class);
        return new RuntimeValue<>(createAggregationRepository(config, agroalDataSource));
    }

    public RuntimeValue<JdbcMessageIdRepository> createIdempotentRepository(
            String dsName, String prefix, RuntimeValue<CamelContext> camelContext) {

        DataSourceFactoryConfig config = new DataSourceFactoryConfig(prefix);
        CamelContext context = camelContext.getValue();
        DataSource agroalDataSource = context.getRegistry().lookupByNameAndType(dsName, DataSource.class);
        return new RuntimeValue<>(createIdempotentRepository(config, agroalDataSource));
    }

    private JdbcAggregationRepository createAggregationRepository(
            DataSourceFactoryConfig dsFactoryConfig, DataSource agroalDataSource) {
        return new ForageAggregationRepository(
                agroalDataSource, com.arjuna.ats.jta.TransactionManager.transactionManager(), dsFactoryConfig);
    }

    private JdbcMessageIdRepository createIdempotentRepository(
            DataSourceFactoryConfig config, DataSource agroalDataSource) {

        ForageIdRepository forageIdRepository =
                switch (config.dbKind()) {
                    case "db2" -> new Db2Jdbc();
                    case "h2" -> new H2Jdbc();
                    case "hsqldb" -> new HsqldbJdbc();
                    case "mariadb" -> new MariadbJdbc();
                    case "mssql" -> new MssqlJdbc();
                    case "mysql" -> new MysqlJdbc();
                    case "oracle" -> new OracleJdbc();
                    case "postgresql" -> new PostgresqlJdbc();
                    default ->
                        throw new IllegalStateException(
                                "Unsupported db kind '%s' for idempotent repository".formatted(config.dbKind()));
                };

        return new ForageJdbcMessageIdRepository(config, agroalDataSource, forageIdRepository);
    }
}
