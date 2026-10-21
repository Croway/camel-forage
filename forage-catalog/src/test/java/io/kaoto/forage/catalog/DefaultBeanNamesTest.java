package io.kaoto.forage.catalog;

import java.lang.reflect.Field;
import java.util.function.Supplier;
import java.util.stream.Stream;
import io.kaoto.forage.agent.AgentBeanFactory;
import io.kaoto.forage.agent.AgentModuleDescriptor;
import io.kaoto.forage.core.annotations.ForageFactory;
import io.kaoto.forage.cxf.CxfBeanFactory;
import io.kaoto.forage.cxf.common.CxfModuleDescriptor;
import io.kaoto.forage.jdbc.DataSourceBeanFactory;
import io.kaoto.forage.jdbc.common.JdbcModuleDescriptor;
import io.kaoto.forage.jms.ConnectionFactoryBeanFactory;
import io.kaoto.forage.jms.common.JmsModuleDescriptor;
import io.kaoto.forage.messaging.spring.rabbitmq.SpringRabbitMQBeanFactory;
import io.kaoto.forage.messaging.spring.rabbitmq.common.SpringRabbitMQConstants;
import io.kaoto.forage.security.tls.TlsBeanFactory;
import io.kaoto.forage.websearch.WebSearchBeanFactory;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that the {@code defaultBeanName} declared on {@link ForageFactory},
 * which ends up in the catalog and in {@code camel forage config read}, matches the name the runtime
 * actually registers the bean under.
 */
class DefaultBeanNamesTest {

    static Stream<Arguments> factories() {
        return Stream.of(
                Arguments.of(DataSourceBeanFactory.class, supply(() -> new JdbcModuleDescriptor().defaultBeanName())),
                Arguments.of(
                        ConnectionFactoryBeanFactory.class, supply(() -> new JmsModuleDescriptor().defaultBeanName())),
                Arguments.of(AgentBeanFactory.class, supply(() -> new AgentModuleDescriptor().defaultBeanName())),
                Arguments.of(CxfBeanFactory.class, supply(() -> new CxfModuleDescriptor().defaultBeanName())),
                Arguments.of(SpringRabbitMQBeanFactory.class, supply(() -> SpringRabbitMQConstants.DEFAULT_BEAN_NAME)),
                Arguments.of(TlsBeanFactory.class, supply(() -> defaultBeanNameConstant(TlsBeanFactory.class))),
                Arguments.of(
                        WebSearchBeanFactory.class, supply(() -> defaultBeanNameConstant(WebSearchBeanFactory.class))));
    }

    @ParameterizedTest
    @MethodSource("factories")
    void factoryDefaultBeanNameMatchesRuntime(Class<?> factoryClass, Supplier<String> runtimeName) {
        assertThat(factoryClass.getAnnotation(ForageFactory.class).defaultBeanName())
                .as("@ForageFactory(defaultBeanName) on %s", factoryClass.getSimpleName())
                .isEqualTo(runtimeName.get());
    }

    private static Supplier<String> supply(Supplier<String> supplier) {
        return supplier;
    }

    private static String defaultBeanNameConstant(Class<?> factoryClass) {
        try {
            Field field = factoryClass.getDeclaredField("DEFAULT_BEAN_NAME");
            field.setAccessible(true);
            return (String) field.get(null);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("No DEFAULT_BEAN_NAME constant on " + factoryClass.getName(), e);
        }
    }
}
