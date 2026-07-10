package io.kaoto.forage.springboot.common.jta;

import java.util.Map;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.SystemEnvironmentPropertySource;
import io.kaoto.forage.core.util.config.ConfigStore;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ForageTransactionEnabledConditionTest {

    @Configuration
    @ConditionalOnAnyForageTransactionEnabled(modulePrefix = "jdbc")
    static class JdbcTxConfig {
        @Bean
        String jdbcTxMarker() {
            return "jdbc-tx-active";
        }
    }

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner().withUserConfiguration(JdbcTxConfig.class);

    @Test
    void defaultKeyActivatesCondition() {
        contextRunner.withPropertyValues("forage.jdbc.transaction.enabled=true").run(ctx -> assertThat(ctx)
                .hasBean("jdbcTxMarker"));
    }

    @Test
    void prefixedKeyActivatesCondition() {
        contextRunner
                .withPropertyValues("forage.ds1.jdbc.transaction.enabled=true")
                .run(ctx -> assertThat(ctx).hasBean("jdbcTxMarker"));
    }

    @Test
    void conditionDoesNotMatchWhenFalse() {
        contextRunner
                .withPropertyValues("forage.jdbc.transaction.enabled=false")
                .run(ctx -> assertThat(ctx).doesNotHaveBean("jdbcTxMarker"));
    }

    @Test
    void conditionDoesNotMatchWhenAbsent() {
        contextRunner.run(ctx -> assertThat(ctx).doesNotHaveBean("jdbcTxMarker"));
    }

    @Test
    void envVarStyleDefaultKeyActivatesCondition() {
        contextRunner
                .withInitializer(ctx -> ctx.getEnvironment()
                        .getPropertySources()
                        .addFirst(new SystemEnvironmentPropertySource(
                                "testSystemEnvironment", Map.of("FORAGE_JDBC_TRANSACTION_ENABLED", "true"))))
                .run(ctx -> assertThat(ctx).hasBean("jdbcTxMarker"));
    }

    @Test
    void envVarStylePrefixedKeyActivatesCondition() {
        contextRunner
                .withInitializer(ctx -> ctx.getEnvironment()
                        .getPropertySources()
                        .addFirst(new SystemEnvironmentPropertySource(
                                "testSystemEnvironment", Map.of("FORAGE_DS1_JDBC_TRANSACTION_ENABLED", "true"))))
                .run(ctx -> assertThat(ctx).hasBean("jdbcTxMarker"));
    }

    @Test
    void envVarStyleKeyDoesNotMatchWhenFalse() {
        contextRunner
                .withInitializer(ctx -> ctx.getEnvironment()
                        .getPropertySources()
                        .addFirst(new SystemEnvironmentPropertySource(
                                "testSystemEnvironment", Map.of("FORAGE_JDBC_TRANSACTION_ENABLED", "false"))))
                .run(ctx -> assertThat(ctx).doesNotHaveBean("jdbcTxMarker"));
    }

    @Test
    void configStoreOnlyPropertyActivatesCondition() {
        // Simulates a forage-*.properties file loaded only into Forage's ConfigStore,
        // not into the Spring Environment
        ConfigStore.getInstance().setDirect("forage.csonly.jdbc.transaction.enabled", "true");
        try {
            contextRunner.run(ctx -> assertThat(ctx).hasBean("jdbcTxMarker"));
        } finally {
            ConfigStore.getInstance().setDirect("forage.csonly.jdbc.transaction.enabled", "false");
        }
    }
}
