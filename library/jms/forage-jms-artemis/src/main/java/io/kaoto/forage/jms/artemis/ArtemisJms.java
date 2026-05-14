package io.kaoto.forage.jms.artemis;

import jakarta.jms.ConnectionFactory;
import jakarta.jms.XAConnectionFactory;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.apache.activemq.artemis.api.core.TransportConfiguration;
import org.apache.activemq.artemis.core.remoting.impl.netty.TransportConstants;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.apache.activemq.artemis.jms.client.ActiveMQXAConnectionFactory;
import io.kaoto.forage.core.annotations.ForageBean;
import io.kaoto.forage.jms.common.ConnectionFactoryConfig;
import io.kaoto.forage.jms.common.PooledConnectionFactory;

@ForageBean(
        value = "artemis",
        components = {"camel-jms"},
        description = "ActiveMQ Artemis message broker",
        feature = "jakarta.jms.ConnectionFactory",
        configClass = ArtemisConfig.class,
        runtimeDependencies = {"quarkus:mvn:io.quarkiverse.artemis:quarkus-artemis-jms"})
public class ArtemisJms extends PooledConnectionFactory {

    @Override
    protected ConnectionFactory createConnectionFactory(ConnectionFactoryConfig config) {
        ArtemisConfig artemisConfig = new ArtemisConfig(config.configPrefix());
        String brokerUrl = buildBrokerUrl(config);
        ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(brokerUrl);

        injectSslPasswords(connectionFactory, config);
        setupConnection(config, artemisConfig, connectionFactory);

        return connectionFactory;
    }

    @Override
    protected XAConnectionFactory createXAConnectionFactory(ConnectionFactoryConfig config) {
        ArtemisConfig artemisConfig = new ArtemisConfig(config.configPrefix());
        String brokerUrl = buildBrokerUrl(config);
        ActiveMQXAConnectionFactory xaConnectionFactory = new ActiveMQXAConnectionFactory(brokerUrl);

        injectSslPasswords(xaConnectionFactory, config);
        setupConnection(config, artemisConfig, xaConnectionFactory);

        return xaConnectionFactory;
    }

    private static void setupConnection(
            ConnectionFactoryConfig config, ArtemisConfig artemisConfig, ActiveMQConnectionFactory connectionFactory) {
        if (config.username() != null) {
            connectionFactory.setUser(config.username());
        }
        if (config.password() != null) {
            connectionFactory.setPassword(config.password());
        }
        if (config.clientId() != null) {
            connectionFactory.setClientID(config.clientId());
        }

        connectionFactory.setReconnectAttempts(artemisConfig.reconnectAttempts());
        connectionFactory.setInitialConnectAttempts(artemisConfig.initialConnectAttempts());
        connectionFactory.setRetryInterval(artemisConfig.retryInterval());

        connectionFactory.setConsumerWindowSize(artemisConfig.consumerWindowSize());
        connectionFactory.setProducerWindowSize(artemisConfig.producerWindowSize());
        connectionFactory.setConsumerMaxRate(artemisConfig.consumerMaxRate());
        connectionFactory.setProducerMaxRate(artemisConfig.producerMaxRate());

        connectionFactory.setMinLargeMessageSize(artemisConfig.minLargeMessageSize());
        connectionFactory.setCompressLargeMessage(artemisConfig.compressLargeMessages());

        connectionFactory.setCallTimeout(artemisConfig.callTimeout());
        connectionFactory.setConnectionTTL(artemisConfig.connectionTtl());
    }

    private static void injectSslPasswords(ActiveMQConnectionFactory factory, ConnectionFactoryConfig config) {
        if (!config.sslEnabled()) {
            return;
        }
        for (TransportConfiguration tc : factory.getStaticConnectors()) {
            Map<String, Object> params = tc.getParams();
            if (config.sslTruststorePassword() != null) {
                params.put(TransportConstants.TRUSTSTORE_PASSWORD_PROP_NAME, config.sslTruststorePassword());
            }
            if (config.sslKeystorePassword() != null) {
                params.put(TransportConstants.KEYSTORE_PASSWORD_PROP_NAME, config.sslKeystorePassword());
            }
        }
    }

    private static String buildBrokerUrl(ConnectionFactoryConfig config) {
        String baseUrl = config.brokerUrl();
        if (!config.sslEnabled()) {
            return baseUrl;
        }

        StringBuilder sb = new StringBuilder(baseUrl);
        sb.append(baseUrl.contains("?") ? "&" : "?");
        sb.append("sslEnabled=true");
        appendIfNotNull(sb, "trustStorePath", config.sslTruststorePath());
        appendIfNotNull(sb, "trustStoreType", config.sslTruststoreType());
        appendIfNotNull(sb, "keyStorePath", config.sslKeystorePath());
        appendIfNotNull(sb, "keyStoreType", config.sslKeystoreType());
        appendIfNotNull(sb, "enabledCipherSuites", config.sslCipherSuites());
        appendIfNotNull(sb, "enabledProtocols", config.sslProtocol());
        return sb.toString();
    }

    private static void appendIfNotNull(StringBuilder sb, String key, String value) {
        if (value != null) {
            sb.append("&").append(key).append("=").append(URLEncoder.encode(value, StandardCharsets.UTF_8));
        }
    }
}
