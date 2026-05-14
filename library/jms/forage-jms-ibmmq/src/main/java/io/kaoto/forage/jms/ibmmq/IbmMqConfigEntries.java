package io.kaoto.forage.jms.ibmmq;

import io.kaoto.forage.core.util.config.ConfigEntries;
import io.kaoto.forage.core.util.config.ConfigModule;
import io.kaoto.forage.core.util.config.ConfigTag;

public final class IbmMqConfigEntries extends ConfigEntries {

    public static final ConfigModule CCDT_URL = ConfigModule.of(
            IbmMqConfig.class,
            "forage.jms.ibmmq.ccdt.url",
            "URL or file path to the JSON CCDT for IBM MQ connection definitions",
            "CCDT URL",
            null,
            "string",
            false,
            ConfigTag.ADVANCED);

    public static final ConfigModule RECONNECT_OPTION = ConfigModule.of(
            IbmMqConfig.class,
            "forage.jms.ibmmq.reconnect.option",
            "Client reconnection option (NO, YES, QMGR, DISABLED)",
            "Reconnect Option",
            "NO",
            "string",
            false,
            ConfigTag.ADVANCED);

    public static final ConfigModule RECONNECT_TIMEOUT = ConfigModule.of(
            IbmMqConfig.class,
            "forage.jms.ibmmq.reconnect.timeout",
            "Reconnection timeout in seconds",
            "Reconnect Timeout",
            "1800",
            "integer",
            false,
            ConfigTag.ADVANCED);

    public static final ConfigModule SHARE_CONVERSATIONS = ConfigModule.of(
            IbmMqConfig.class,
            "forage.jms.ibmmq.share.conversations",
            "Whether shared conversations are allowed per connection (0 = NO, 1 = YES). If not set, the IBM MQ client default is used.",
            "Share Conversations",
            null,
            "integer",
            false,
            ConfigTag.ADVANCED);

    public static final ConfigModule SSL_CIPHER_SUITE = ConfigModule.of(
            IbmMqConfig.class,
            "forage.jms.ibmmq.ssl.cipher.suite",
            "IBM MQ-specific cipher suite name (uses IBM naming, not JSSE)",
            "IBM MQ Cipher Suite",
            null,
            "string",
            false,
            ConfigTag.SECURITY);

    public static final ConfigModule SSL_PEER_NAME = ConfigModule.of(
            IbmMqConfig.class,
            "forage.jms.ibmmq.ssl.peer.name",
            "Distinguished name pattern for SSL peer verification",
            "SSL Peer Name",
            null,
            "string",
            false,
            ConfigTag.SECURITY);

    static {
        initModules(
                IbmMqConfigEntries.class,
                CCDT_URL,
                RECONNECT_OPTION,
                RECONNECT_TIMEOUT,
                SHARE_CONVERSATIONS,
                SSL_CIPHER_SUITE,
                SSL_PEER_NAME);
    }
}
