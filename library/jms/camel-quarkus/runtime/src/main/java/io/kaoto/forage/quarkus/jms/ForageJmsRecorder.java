package io.kaoto.forage.quarkus.jms;

import jakarta.jms.ConnectionFactory;

import org.apache.camel.spi.ComponentCustomizer;
import org.jboss.logging.Logger;
import io.kaoto.forage.jms.common.transactions.JmsJtaTransactionSupport;
import io.kaoto.forage.jms.ibmmq.IbmMqJms;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;

/**
 * Aggregation repository is created via Recorder
 */
@Recorder
public class ForageJmsRecorder {
    private static final Logger LOG = Logger.getLogger(ForageJmsRecorder.class);

    public RuntimeValue<ConnectionFactory> createIbmMQConnectionFactory(String id) {
        ConnectionFactory cf = new IbmMqJms().create(id);
        if (cf == null) {
            throw new IllegalStateException("IBM MQ ConnectionFactory could not be created for id '%s'".formatted(id));
        }
        return new RuntimeValue<>(cf);
    }

    /**
     * Creates a {@link ComponentCustomizer} that wires a Spring {@code JtaTransactionManager}
     * (wrapping the Narayana transaction manager) into the Camel JMS component, so JMS
     * consumers receive within a JTA transaction and XA sessions enlist in it (#427).
     */
    public RuntimeValue<ComponentCustomizer> createJmsTransactionManagerCustomizer() {
        LOG.info("Registering Forage JTA transaction manager customizer for the Camel JMS component");
        return new RuntimeValue<>(JmsJtaTransactionSupport.jmsComponentCustomizer(
                JmsJtaTransactionSupport.createJtaTransactionManager()));
    }
}
