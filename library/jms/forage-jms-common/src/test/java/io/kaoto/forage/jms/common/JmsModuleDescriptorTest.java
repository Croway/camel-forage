package io.kaoto.forage.jms.common;

import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class JmsModuleDescriptorTest {

    @BeforeEach
    void setUp() {
        System.setProperty("forage.jms.kind", "artemis");
        System.setProperty("forage.jms.broker.url", "tcp://localhost:61616");
        System.setProperty("forage.jms.pool.block.if.full.timeout.millis", "-1");
    }

    @AfterEach
    void tearDown() {
        System.clearProperty("forage.jms.kind");
        System.clearProperty("forage.jms.broker.url");
        System.clearProperty("forage.jms.pool.block.if.full.timeout.millis");
    }

    @Test
    void blockIfFullTimeoutSentinelNotCorrupted() {
        ConnectionFactoryConfig config = new ConnectionFactoryConfig();
        Map<String, String> props = new JmsModuleDescriptor().translateProperties(null, config);

        // -1 (infinite sentinel) must not be divided by 1000 and must be passed through as-is
        assertThat(props.get("quarkus.pooled-jms.block-if-session-pool-is-full-timeout"))
                .isEqualTo("-1");
    }
}
