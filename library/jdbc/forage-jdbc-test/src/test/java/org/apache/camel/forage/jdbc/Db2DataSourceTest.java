package org.apache.camel.forage.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import org.apache.camel.forage.core.jdbc.DataSourceProvider;
import org.apache.camel.forage.jdbc.db2.Db2Jdbc;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers(disabledWithoutDocker = true)
@DisabledOnOs(OS.MAC) // The test is really slow on mac
@DisabledIfSystemProperty(named = "ci.env.name", matches = ".*", disabledReason = "Slow or flaky on GitHub action")
public class Db2DataSourceTest extends DataSourceTest {

    @Container
    static GenericContainer<?> db2 = new GenericContainer<>(DockerImageName.parse("icr.io/db2_community/db2"))
            .withExposedPorts(50000)
            .withEnv("LICENSE", "accept")
            .withEnv("DB2INSTANCE", "db2inst1")
            .withEnv("DB2INST1_PASSWORD", "password")
            .withEnv("DBNAME", "testdb")
            .withPrivilegedMode(true)
            .withStartupTimeout(Duration.ofSeconds(900))
            .waitingFor(Wait.forLogMessage(".*All databases are now active.*", 1)
                    .withStartupTimeout(Duration.ofSeconds(900)));

    @Override
    protected DataSourceProvider createDataSourceProvider() {
        return new Db2Jdbc();
    }

    @Override
    protected void setUpDataSource(String dataSourceName) {
        System.setProperty(dataSourceName + ".jdbc.db.kind", "db2");
        System.setProperty(
                dataSourceName + ".jdbc.url", "jdbc:db2://localhost:" + db2.getMappedPort(50000) + "/testdb");
        System.setProperty(dataSourceName + ".jdbc.username", "db2inst1");
        System.setProperty(dataSourceName + ".jdbc.password", "password");
    }

    @Override
    protected void validateTestQueryResult(ResultSet rs) throws SQLException {
        rs.next();

        Assertions.assertThat(rs.getString(1)).contains("DB2");
    }

    @Test
    public void testDefaultTransactedDataSource() throws Exception {
        // XA Transactions do not work with db2 docker image
    }
}
