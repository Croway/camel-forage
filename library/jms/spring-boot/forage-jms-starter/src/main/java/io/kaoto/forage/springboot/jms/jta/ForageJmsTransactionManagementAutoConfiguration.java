package io.kaoto.forage.springboot.jms.jta;

import jakarta.annotation.PostConstruct;

import org.apache.camel.spi.ComponentCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.jta.JtaTransactionManager;
import io.kaoto.forage.jms.common.transactions.JmsJtaTransactionSupport;
import io.kaoto.forage.springboot.common.jta.ConditionalOnAnyForageTransactionEnabled;

@Configuration
@ConditionalOnAnyForageTransactionEnabled(modulePrefix = "jms")
@EnableTransactionManagement
public class ForageJmsTransactionManagementAutoConfiguration
        extends io.kaoto.forage.springboot.common.jta.ForageTransactionManagementAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(ForageJmsTransactionManagementAutoConfiguration.class);

    @PostConstruct
    public void init() {
        log.info("ForageJmsTransactionManagementAutoConfiguration initialized - transaction management enabled");
    }

    /**
     * Sets the JTA transaction manager on the Camel JMS component so the message listener
     * container starts a JTA transaction around {@code receive()} and XA sessions enlist in
     * it (#427). Applied by Camel to every {@code JmsComponent} added to the context.
     */
    @Bean
    public ComponentCustomizer forageJmsTransactionManagerCustomizer(JtaTransactionManager jtaTransactionManager) {
        return JmsJtaTransactionSupport.jmsComponentCustomizer(jtaTransactionManager);
    }
}
