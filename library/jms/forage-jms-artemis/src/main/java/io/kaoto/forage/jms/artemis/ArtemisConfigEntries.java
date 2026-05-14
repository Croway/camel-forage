package io.kaoto.forage.jms.artemis;

import io.kaoto.forage.core.util.config.ConfigEntries;
import io.kaoto.forage.core.util.config.ConfigModule;
import io.kaoto.forage.core.util.config.ConfigTag;

public final class ArtemisConfigEntries extends ConfigEntries {

    public static final ConfigModule RECONNECT_ATTEMPTS = ConfigModule.of(
            ArtemisConfig.class,
            "forage.jms.artemis.reconnect.attempts",
            "Number of reconnection attempts for established sessions (-1 for infinite)",
            "Reconnect Attempts",
            "0",
            "integer",
            false,
            ConfigTag.ADVANCED);

    public static final ConfigModule INITIAL_CONNECT_ATTEMPTS = ConfigModule.of(
            ArtemisConfig.class,
            "forage.jms.artemis.initial.connect.attempts",
            "Number of initial connection attempts before cluster topology is known",
            "Initial Connect Attempts",
            "0",
            "integer",
            false,
            ConfigTag.ADVANCED);

    public static final ConfigModule RETRY_INTERVAL = ConfigModule.of(
            ArtemisConfig.class,
            "forage.jms.artemis.retry.interval",
            "Delay between reconnection attempts (milliseconds)",
            "Retry Interval",
            "2000",
            "integer",
            false,
            ConfigTag.ADVANCED);

    public static final ConfigModule CONSUMER_WINDOW_SIZE = ConfigModule.of(
            ArtemisConfig.class,
            "forage.jms.artemis.consumer.window.size",
            "Consumer pre-fetch buffer size in bytes (-1 unbounded, 0 disables buffering)",
            "Consumer Window Size",
            "1048576",
            "integer",
            false,
            ConfigTag.ADVANCED);

    public static final ConfigModule PRODUCER_WINDOW_SIZE = ConfigModule.of(
            ArtemisConfig.class,
            "forage.jms.artemis.producer.window.size",
            "Producer flow control window size in bytes",
            "Producer Window Size",
            "65536",
            "integer",
            false,
            ConfigTag.ADVANCED);

    public static final ConfigModule CONSUMER_MAX_RATE = ConfigModule.of(
            ArtemisConfig.class,
            "forage.jms.artemis.consumer.max.rate",
            "Maximum consumer message rate (messages/second, -1 for unlimited)",
            "Consumer Max Rate",
            "-1",
            "integer",
            false,
            ConfigTag.ADVANCED);

    public static final ConfigModule PRODUCER_MAX_RATE = ConfigModule.of(
            ArtemisConfig.class,
            "forage.jms.artemis.producer.max.rate",
            "Maximum producer message rate (messages/second, -1 for unlimited)",
            "Producer Max Rate",
            "-1",
            "integer",
            false,
            ConfigTag.ADVANCED);

    public static final ConfigModule MIN_LARGE_MESSAGE_SIZE = ConfigModule.of(
            ArtemisConfig.class,
            "forage.jms.artemis.min.large.message.size",
            "Minimum size in bytes for large message streaming to disk",
            "Min Large Message Size",
            "102400",
            "integer",
            false,
            ConfigTag.ADVANCED);

    public static final ConfigModule COMPRESS_LARGE_MESSAGES = ConfigModule.of(
            ArtemisConfig.class,
            "forage.jms.artemis.compress.large.messages",
            "Enable compression for large messages",
            "Compress Large Messages",
            "false",
            "boolean",
            false,
            ConfigTag.ADVANCED);

    public static final ConfigModule CALL_TIMEOUT = ConfigModule.of(
            ArtemisConfig.class,
            "forage.jms.artemis.call.timeout",
            "Timeout for blocking calls (milliseconds)",
            "Call Timeout",
            "30000",
            "integer",
            false,
            ConfigTag.ADVANCED);

    public static final ConfigModule CONNECTION_TTL = ConfigModule.of(
            ArtemisConfig.class,
            "forage.jms.artemis.connection.ttl",
            "Connection time-to-live (milliseconds)",
            "Connection TTL",
            "60000",
            "integer",
            false,
            ConfigTag.ADVANCED);

    static {
        initModules(
                ArtemisConfigEntries.class,
                RECONNECT_ATTEMPTS,
                INITIAL_CONNECT_ATTEMPTS,
                RETRY_INTERVAL,
                CONSUMER_WINDOW_SIZE,
                PRODUCER_WINDOW_SIZE,
                CONSUMER_MAX_RATE,
                PRODUCER_MAX_RATE,
                MIN_LARGE_MESSAGE_SIZE,
                COMPRESS_LARGE_MESSAGES,
                CALL_TIMEOUT,
                CONNECTION_TTL);
    }
}
