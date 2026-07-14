package io.kaoto.forage.messaging.springrabbitmq;

import org.testcontainers.rabbitmq.RabbitMQContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Singleton Testcontainers shared by all test classes and runtime suites in this module.
 *
 * <p>The three runtime suites (plain, Quarkus, Spring Boot) run the same test classes within one
 * JVM, so class-scoped {@code @Container} fields restarted RabbitMQ once per class per suite (9
 * times per run). The broker here starts lazily on first use and lives until the JVM exits, where
 * Testcontainers' Ryuk reaps it (#434).
 *
 * <p>Because the broker survives across classes and suites, queues may hold leftover messages from
 * a previous class's producer: assertions have to verify each test's freshly started integration
 * processes (e.g. via their logs), not broker contents.
 */
final class MessagingContainers {

    private static final RabbitMQContainer RABBITMQ =
            new RabbitMQContainer(DockerImageName.parse("rabbitmq:3.13-management")).withExposedPorts(5672, 15672);

    private MessagingContainers() {}

    static synchronized RabbitMQContainer rabbitmq() {
        if (!RABBITMQ.isRunning()) {
            RABBITMQ.start();
        }
        return RABBITMQ;
    }
}
