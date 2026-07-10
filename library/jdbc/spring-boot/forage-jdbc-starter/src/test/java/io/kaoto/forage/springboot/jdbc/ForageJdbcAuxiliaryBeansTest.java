package io.kaoto.forage.springboot.jdbc;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import io.agroal.api.AgroalDataSource;
import io.kaoto.forage.jdbc.common.aggregation.ForageAggregationRepository;
import io.kaoto.forage.jdbc.common.idempotent.ForageJdbcMessageIdRepository;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that auxiliary JDBC beans (aggregation repository, idempotent repository)
 * registered for a prefixed datasource reuse the Spring-managed DataSource bean instead
 * of opening a second connection pool (issue #404).
 *
 * <p>Uses the H2 provider from the test classpath with an in-memory database.
 */
class ForageJdbcAuxiliaryBeansTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ForageDataSourceAutoConfiguration.class));

    @Test
    void auxiliaryRepositoriesUseTheRegisteredDataSourceBean() {
        contextRunner
                .withSystemProperties(
                        "forage.ds1.jdbc.db.kind=h2",
                        "forage.ds1.jdbc.url=jdbc:h2:mem:auxbeans;DB_CLOSE_DELAY=-1",
                        "forage.ds1.jdbc.username=sa",
                        "forage.ds1.jdbc.password=sa",
                        "forage.ds1.jdbc.transaction.enabled=true",
                        "forage.ds1.jdbc.transaction.object.store.directory=target/narayana-objectstore",
                        "forage.ds1.jdbc.aggregation.repository.name=auxAggregationRepo",
                        "forage.ds1.jdbc.idempotent.repository.enabled=true",
                        "forage.ds1.jdbc.idempotent.repository.table.name=AUX_IDEMPOTENT")
                .run(ctx -> {
                    assertThat(ctx).hasNotFailed();

                    AgroalDataSource dataSource = ctx.getBean("ds1", AgroalDataSource.class);

                    // The first prefix is aliased as the default bean name
                    assertThat(ctx.getBean("dataSource")).isSameAs(dataSource);

                    ForageAggregationRepository aggregationRepository =
                            ctx.getBean("auxAggregationRepo", ForageAggregationRepository.class);
                    assertThat(aggregationRepository.getDataSource())
                            .as("aggregation repository must reuse the registered DataSource bean")
                            .isSameAs(dataSource);

                    ForageJdbcMessageIdRepository idempotentRepository =
                            ctx.getBean("AUX_IDEMPOTENT", ForageJdbcMessageIdRepository.class);
                    assertThat(idempotentRepository.getDataSource())
                            .as("idempotent repository must reuse the registered DataSource bean")
                            .isSameAs(dataSource);
                });
    }
}
