package io.kaoto.forage.jdbc.common;

import io.kaoto.forage.jdbc.common.idempotent.ForageJdbcMessageIdRepository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.parallel.Resources.SYSTEM_PROPERTIES;

@DisplayName("DataSourceFactoryConfig Tests")
@ResourceLock(SYSTEM_PROPERTIES)
class DataSourceFactoryConfigTest {

    private static final String AGGREGATION_NAME_PROPERTY = "forage.blankNames.jdbc.aggregation.repository.name";
    private static final String IDEMPOTENT_TABLE_PROPERTY = "forage.blankNames.jdbc.idempotent.repository.table.name";

    @AfterEach
    void clearProperties() {
        System.clearProperty(AGGREGATION_NAME_PROPERTY);
        System.clearProperty(IDEMPOTENT_TABLE_PROPERTY);
    }

    @Test
    @DisplayName("Configured repository names are returned")
    void configuredRepositoryNamesAreReturned() {
        System.setProperty(AGGREGATION_NAME_PROPERTY, "orderAggRepo");
        System.setProperty(IDEMPOTENT_TABLE_PROPERTY, "ORDER_IDEMPOTENT");

        DataSourceFactoryConfig config = new DataSourceFactoryConfig("blankNames");

        assertThat(config.aggregationRepositoryName()).isEqualTo("orderAggRepo");
        assertThat(config.idempotentRepositoryTableName()).isEqualTo("ORDER_IDEMPOTENT");
    }

    @Test
    @DisplayName("Blank repository names are treated as unset")
    void blankRepositoryNamesAreTreatedAsUnset() {
        System.setProperty(AGGREGATION_NAME_PROPERTY, " ");
        System.setProperty(IDEMPOTENT_TABLE_PROPERTY, "");

        DataSourceFactoryConfig config = new DataSourceFactoryConfig("blankNames");

        assertThat(config.aggregationRepositoryName()).isNull();
        assertThat(config.idempotentRepositoryTableName()).isEqualTo(ForageJdbcMessageIdRepository.DEFAULT_TABLENAME);
    }
}
