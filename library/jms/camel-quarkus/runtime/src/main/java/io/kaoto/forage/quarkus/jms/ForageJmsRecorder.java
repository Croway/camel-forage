package io.kaoto.forage.quarkus.jms;

import jakarta.jms.ConnectionFactory;

import java.util.Map;
import org.apache.camel.spi.CamelContextCustomizer;
import org.apache.camel.spi.ComponentCustomizer;
import org.jboss.logging.Logger;
import org.springframework.transaction.jta.JtaTransactionManager;
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
     * (wrapping the Narayana transaction manager) into the default Camel JMS component, so
     * JMS consumers receive within a JTA transaction and XA sessions enlist in it (#427).
     * Named components are handled by {@link #createPerBrokerJmsComponentCustomizer} (#433).
     */
    public RuntimeValue<ComponentCustomizer> createJmsTransactionManagerCustomizer() {
        LOG.info("Registering Forage JTA transaction manager customizer for the default Camel JMS component");
        return new RuntimeValue<>(JmsJtaTransactionSupport.jmsComponentCustomizer(
                JmsJtaTransactionSupport.createJtaTransactionManager()));
    }

    /**
     * Creates a {@link CamelContextCustomizer} that registers per-broker {@code JmsComponent}
     * instances so each named broker has its own ConnectionFactory and JTA scoping (#433).
     */
    public RuntimeValue<CamelContextCustomizer> createPerBrokerJmsComponentCustomizer(
            Map<String, Boolean> namedPrefixes) {
        return new RuntimeValue<>(camelContext -> {
            JtaTransactionManager jtaTransactionManager = null;
            boolean anyTransactionEnabled = namedPrefixes.containsValue(Boolean.TRUE);
            if (anyTransactionEnabled) {
                jtaTransactionManager = JmsJtaTransactionSupport.createJtaTransactionManager();
            }

            for (Map.Entry<String, Boolean> entry : namedPrefixes.entrySet()) {
                String name = entry.getKey();
                boolean txEnabled = entry.getValue();
                ConnectionFactory cf = camelContext.getRegistry().lookupByNameAndType(name, ConnectionFactory.class);
                if (cf == null) {
                    LOG.warnf("ConnectionFactory '%s' not found in registry, skipping JmsComponent registration", name);
                    continue;
                }
                JtaTransactionManager perBrokerTm = txEnabled ? jtaTransactionManager : null;
                camelContext.addComponent(name, JmsJtaTransactionSupport.createJmsComponent(cf, perBrokerTm));
                LOG.infof("Registered per-broker JmsComponent '%s' (transactions=%s)", name, txEnabled);
            }
        });
    }
}
