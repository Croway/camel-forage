package io.kaoto.forage.springboot.jms;

import jakarta.jms.ConnectionFactory;

import org.messaginghub.pooled.jms.JmsPoolConnectionFactory;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.ConfigurableApplicationContext;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Lifecycle tests for Forage-registered JMS ConnectionFactory beans.
 *
 * <p>Uses the Artemis provider from the test classpath; the connection factory does not
 * connect to a broker until a connection is requested, so no broker is needed.
 */
class ForageJmsConnectionFactoryLifecycleTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ForageConnectionFactoryAutoConfiguration.class));

    @Test
    void prefixedPooledConnectionFactoryIsStoppedOnContextClose() {
        contextRunner
                .withSystemProperties(
                        "forage.mqpool.jms.kind=artemis", "forage.mqpool.jms.broker.url=tcp://localhost:61616")
                .run(ctx -> {
                    assertThat(ctx).hasNotFailed();

                    ConnectionFactory cf = ctx.getBean("mqpool", ConnectionFactory.class);
                    assertThat(cf).isInstanceOf(JmsPoolConnectionFactory.class);

                    // The first prefix is aliased as the default bean name and must resolve
                    // to the same instance (no duplicate pool)
                    assertThat(ctx.getBean("connectionFactory")).isSameAs(cf);

                    // Closing the context must not throw (no double-destroy through aliases)
                    ConfigurableApplicationContext applicationContext = ctx.getSourceApplicationContext();
                    assertThatCode(applicationContext::close).doesNotThrowAnyException();

                    // destroy-method "stop" must have shut the pool down
                    JmsPoolConnectionFactory pool = (JmsPoolConnectionFactory) cf;
                    assertThat(pool.getNumConnections()).isZero();
                    assertThat(pool.createConnection())
                            .as("a stopped pool must no longer hand out connections")
                            .isNull();
                });
    }

    @Test
    void poolDisabledPrefixedConnectionFactoryStartsAndClosesCleanly() {
        contextRunner
                .withSystemProperties(
                        "forage.mqnopool.jms.kind=artemis",
                        "forage.mqnopool.jms.broker.url=tcp://localhost:61616",
                        "forage.mqnopool.jms.pool.enabled=false")
                .run(ctx -> {
                    // With pooling disabled the provider returns the raw broker ConnectionFactory,
                    // which has no stop() method. The bean definition must not declare a "stop"
                    // destroy method, or Spring fails with BeanDefinitionValidationException.
                    assertThat(ctx).hasNotFailed();

                    ConnectionFactory cf = ctx.getBean("mqnopool", ConnectionFactory.class);
                    assertThat(cf).isNotInstanceOf(JmsPoolConnectionFactory.class);

                    assertThatCode(() -> ctx.getSourceApplicationContext().close())
                            .doesNotThrowAnyException();
                });
    }
}
