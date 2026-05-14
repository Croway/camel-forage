package io.kaoto.forage.jms.ibmmq;

import io.kaoto.forage.core.util.config.AbstractConfig;

import static io.kaoto.forage.jms.ibmmq.IbmMqConfigEntries.CCDT_URL;
import static io.kaoto.forage.jms.ibmmq.IbmMqConfigEntries.RECONNECT_OPTION;
import static io.kaoto.forage.jms.ibmmq.IbmMqConfigEntries.RECONNECT_TIMEOUT;
import static io.kaoto.forage.jms.ibmmq.IbmMqConfigEntries.SHARE_CONVERSATIONS;
import static io.kaoto.forage.jms.ibmmq.IbmMqConfigEntries.SSL_CIPHER_SUITE;
import static io.kaoto.forage.jms.ibmmq.IbmMqConfigEntries.SSL_PEER_NAME;

public class IbmMqConfig extends AbstractConfig {

    public IbmMqConfig() {
        this(null);
    }

    public IbmMqConfig(String prefix) {
        super(prefix, IbmMqConfigEntries.class);
    }

    @Override
    public String name() {
        return "forage-jms-ibmmq";
    }

    public String ccdtUrl() {
        return get(CCDT_URL).orElse(null);
    }

    public String reconnectOption() {
        return get(RECONNECT_OPTION).orElse(RECONNECT_OPTION.defaultValue());
    }

    public int reconnectTimeout() {
        return get(RECONNECT_TIMEOUT).map(Integer::parseInt).orElse(Integer.parseInt(RECONNECT_TIMEOUT.defaultValue()));
    }

    public Integer shareConversations() {
        return get(SHARE_CONVERSATIONS).map(Integer::parseInt).orElse(null);
    }

    public String sslCipherSuite() {
        return get(SSL_CIPHER_SUITE).orElse(null);
    }

    public String sslPeerName() {
        return get(SSL_PEER_NAME).orElse(null);
    }
}
