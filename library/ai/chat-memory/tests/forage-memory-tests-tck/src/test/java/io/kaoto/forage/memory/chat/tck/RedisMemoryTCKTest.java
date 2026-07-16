package io.kaoto.forage.memory.chat.tck;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import io.kaoto.forage.core.ai.ChatMemoryBeanProvider;
import io.kaoto.forage.memory.chat.redis.RedisMemoryBeanProvider;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Test for RedisMemoryFactory using the ChatMemoryFactoryTCK with testcontainers.
 *
 * <p>This test validates the RedisMemoryFactory implementation from the forage-memory-redis
 * module against the comprehensive test suite provided by the TCK. It uses testcontainers
 * to start a real Redis instance for testing.
 *
 * <p>The test ensures that the Redis-based chat memory implementation correctly handles:
 * <ul>
 *   <li>Message persistence across Redis operations</li>
 *   <li>Memory isolation between different conversation IDs</li>
 *   <li>Connection management and error handling</li>
 *   <li>All standard chat memory operations</li>
 * </ul>
 *
 * All actual tests are inherited from ChatMemoryBeanProviderTCK
 *
 * @since 1.0
 */
@Testcontainers(disabledWithoutDocker = true)
class RedisMemoryTCKTest extends ChatMemoryBeanProviderTCK {

    private static final int REDIS_PORT = 6379;
    private static RedisMemoryBeanProvider provider;

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(REDIS_PORT)
            .withCommand("redis-server", "--appendonly", "yes");

    @BeforeAll
    static void setUpRedis() {
        String redisHost = redis.getHost();
        Integer redisPort = redis.getMappedPort(REDIS_PORT);

        System.setProperty("forage.redis.host", redisHost);
        System.setProperty("forage.redis.port", redisPort.toString());
        System.setProperty("forage.redis.database", "0");
        System.setProperty("forage.redis.timeout", "2000");
        System.setProperty("forage.redis.pool.max.total", "8");
        System.setProperty("forage.redis.pool.max.idle", "8");
        System.setProperty("forage.redis.pool.min.idle", "0");

        provider = new RedisMemoryBeanProvider();
    }

    @AfterAll
    static void tearDownRedis() {
        if (provider != null) {
            provider.close();
        }

        System.clearProperty("forage.redis.host");
        System.clearProperty("forage.redis.port");
        System.clearProperty("forage.redis.database");
        System.clearProperty("forage.redis.timeout");
        System.clearProperty("forage.redis.pool.max.total");
        System.clearProperty("forage.redis.pool.max.idle");
        System.clearProperty("forage.redis.pool.min.idle");
    }

    @Override
    protected ChatMemoryBeanProvider createChatMemoryFactory() {
        return provider;
    }

    @Test
    void redisContainerIsRunning() {
        // Verify that the Redis container is properly started
        assert redis.isRunning();
        assert redis.getMappedPort(REDIS_PORT) != null;
    }
}
