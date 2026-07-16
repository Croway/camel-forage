package io.kaoto.forage.quarkus.messaging.springrabbitmq;

import io.kaoto.forage.core.common.ForageModuleDescriptor;
import io.kaoto.forage.core.common.ForageQuarkusConfigSourceAdapter;
import io.kaoto.forage.messaging.spring.rabbitmq.common.SpringRabbitMQConfig;
import io.kaoto.forage.messaging.spring.rabbitmq.common.SpringRabbitMQModuleDescriptor;

/**
 * SmallRye {@link io.smallrye.config.ConfigSourceFactory} that translates Forage Spring RabbitMQ
 * properties into Quarkus properties at config bootstrap time.
 *
 * <p>Delegates all logic to {@link ForageQuarkusConfigSourceAdapter} using the
 * {@link SpringRabbitMQModuleDescriptor}.
 *
 * <p>Registered via {@code META-INF/services/io.smallrye.config.ConfigSourceFactory}.
 *
 * @since 1.4
 */
public class ForageSpringRabbitMQConfigSourceFactory extends ForageQuarkusConfigSourceAdapter<SpringRabbitMQConfig> {

    @Override
    protected ForageModuleDescriptor<SpringRabbitMQConfig, ?> descriptor() {
        return new SpringRabbitMQModuleDescriptor();
    }
}
