package io.kaoto.forage.jms.artemis;

import io.kaoto.forage.core.util.config.AbstractConfig;

import static io.kaoto.forage.jms.artemis.ArtemisConfigEntries.CALL_TIMEOUT;
import static io.kaoto.forage.jms.artemis.ArtemisConfigEntries.COMPRESS_LARGE_MESSAGES;
import static io.kaoto.forage.jms.artemis.ArtemisConfigEntries.CONNECTION_TTL;
import static io.kaoto.forage.jms.artemis.ArtemisConfigEntries.CONSUMER_MAX_RATE;
import static io.kaoto.forage.jms.artemis.ArtemisConfigEntries.CONSUMER_WINDOW_SIZE;
import static io.kaoto.forage.jms.artemis.ArtemisConfigEntries.INITIAL_CONNECT_ATTEMPTS;
import static io.kaoto.forage.jms.artemis.ArtemisConfigEntries.MIN_LARGE_MESSAGE_SIZE;
import static io.kaoto.forage.jms.artemis.ArtemisConfigEntries.PRODUCER_MAX_RATE;
import static io.kaoto.forage.jms.artemis.ArtemisConfigEntries.PRODUCER_WINDOW_SIZE;
import static io.kaoto.forage.jms.artemis.ArtemisConfigEntries.RECONNECT_ATTEMPTS;
import static io.kaoto.forage.jms.artemis.ArtemisConfigEntries.RETRY_INTERVAL;

public class ArtemisConfig extends AbstractConfig {

    public ArtemisConfig() {
        this(null);
    }

    public ArtemisConfig(String prefix) {
        super(prefix, ArtemisConfigEntries.class);
    }

    @Override
    public String name() {
        return "forage-jms-artemis";
    }

    public int reconnectAttempts() {
        return get(RECONNECT_ATTEMPTS)
                .map(Integer::parseInt)
                .orElse(Integer.parseInt(RECONNECT_ATTEMPTS.defaultValue()));
    }

    public int initialConnectAttempts() {
        return get(INITIAL_CONNECT_ATTEMPTS)
                .map(Integer::parseInt)
                .orElse(Integer.parseInt(INITIAL_CONNECT_ATTEMPTS.defaultValue()));
    }

    public int retryInterval() {
        return get(RETRY_INTERVAL).map(Integer::parseInt).orElse(Integer.parseInt(RETRY_INTERVAL.defaultValue()));
    }

    public int consumerWindowSize() {
        return get(CONSUMER_WINDOW_SIZE)
                .map(Integer::parseInt)
                .orElse(Integer.parseInt(CONSUMER_WINDOW_SIZE.defaultValue()));
    }

    public int producerWindowSize() {
        return get(PRODUCER_WINDOW_SIZE)
                .map(Integer::parseInt)
                .orElse(Integer.parseInt(PRODUCER_WINDOW_SIZE.defaultValue()));
    }

    public int consumerMaxRate() {
        return get(CONSUMER_MAX_RATE).map(Integer::parseInt).orElse(Integer.parseInt(CONSUMER_MAX_RATE.defaultValue()));
    }

    public int producerMaxRate() {
        return get(PRODUCER_MAX_RATE).map(Integer::parseInt).orElse(Integer.parseInt(PRODUCER_MAX_RATE.defaultValue()));
    }

    public int minLargeMessageSize() {
        return get(MIN_LARGE_MESSAGE_SIZE)
                .map(Integer::parseInt)
                .orElse(Integer.parseInt(MIN_LARGE_MESSAGE_SIZE.defaultValue()));
    }

    public boolean compressLargeMessages() {
        return get(COMPRESS_LARGE_MESSAGES)
                .map(Boolean::parseBoolean)
                .orElse(Boolean.parseBoolean(COMPRESS_LARGE_MESSAGES.defaultValue()));
    }

    public int callTimeout() {
        return get(CALL_TIMEOUT).map(Integer::parseInt).orElse(Integer.parseInt(CALL_TIMEOUT.defaultValue()));
    }

    public int connectionTtl() {
        return get(CONNECTION_TTL).map(Integer::parseInt).orElse(Integer.parseInt(CONNECTION_TTL.defaultValue()));
    }
}
