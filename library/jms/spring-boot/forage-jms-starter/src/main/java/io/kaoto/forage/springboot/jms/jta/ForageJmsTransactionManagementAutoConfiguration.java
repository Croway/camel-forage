package io.kaoto.forage.springboot.jms.jta;

import jakarta.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;
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
}
