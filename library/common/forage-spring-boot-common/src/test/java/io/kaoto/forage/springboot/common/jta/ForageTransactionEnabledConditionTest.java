package io.kaoto.forage.springboot.common.jta;

import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
}
