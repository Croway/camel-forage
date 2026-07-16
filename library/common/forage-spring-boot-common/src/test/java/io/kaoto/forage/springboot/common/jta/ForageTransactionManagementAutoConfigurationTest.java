package io.kaoto.forage.springboot.common.jta;

import org.apache.camel.spring.spi.SpringTransactionPolicy;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.jta.JtaTransactionManager;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ForageTransactionManagementAutoConfigurationTest {

    @Configuration
    static class TestTransactionConfig extends ForageTransactionManagementAutoConfiguration {}

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner().withUserConfiguration(TestTransactionConfig.class);

    @Test
    void allSevenPolicyBeansAreRegistered() {
        contextRunner.run(ctx -> {
            assertThat(ctx).hasSingleBean(JtaTransactionManager.class);
            assertThat(ctx.getBean("PROPAGATION_REQUIRED")).isInstanceOf(SpringTransactionPolicy.class);
            assertThat(ctx.getBean("NESTED")).isInstanceOf(SpringTransactionPolicy.class);
            assertThat(ctx.getBean("MANDATORY")).isInstanceOf(SpringTransactionPolicy.class);
            assertThat(ctx.getBean("NEVER")).isInstanceOf(SpringTransactionPolicy.class);
            assertThat(ctx.getBean("NOT_SUPPORTED")).isInstanceOf(SpringTransactionPolicy.class);
            assertThat(ctx.getBean("REQUIRES_NEW")).isInstanceOf(SpringTransactionPolicy.class);
            assertThat(ctx.getBean("SUPPORTS")).isInstanceOf(SpringTransactionPolicy.class);
        });
    }

    @Test
    void recoveryShutdownBeanIsRegistered() {
        contextRunner.run(ctx -> assertThat(ctx).hasSingleBean(ForageRecoveryShutdownBean.class));
    }

    @Test
    void userDefinedPolicyBeanIsNotOverridden() {
        SpringTransactionPolicy custom = new SpringTransactionPolicy();
        ApplicationContextRunner runnerWithOverride = new ApplicationContextRunner()
                .withUserConfiguration(TestTransactionConfig.class)
                .withBean("REQUIRES_NEW", SpringTransactionPolicy.class, () -> custom);

        runnerWithOverride.run(ctx -> {
            assertThat(ctx.getBean("REQUIRES_NEW")).isSameAs(custom);
            // All other policy beans still registered
            assertThat(ctx.getBean("PROPAGATION_REQUIRED")).isInstanceOf(SpringTransactionPolicy.class);
            assertThat(ctx.getBean("SUPPORTS")).isInstanceOf(SpringTransactionPolicy.class);
        });
    }
}
