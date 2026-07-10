package io.kaoto.forage.springboot.jdbc.jta;

import jakarta.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import io.kaoto.forage.springboot.common.jta.ConditionalOnAnyForageTransactionEnabled;

@Configuration
@ConditionalOnAnyForageTransactionEnabled(modulePrefix = "jdbc")
@EnableTransactionManagement
public class ForageJdbcTransactionManagementAutoConfiguration
        extends io.kaoto.forage.springboot.common.jta.ForageTransactionManagementAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(ForageJdbcTransactionManagementAutoConfiguration.class);

    @PostConstruct
    public void init() {
        log.info("ForageJdbcTransactionManagementAutoConfiguration initialized - transaction management enabled");
    }
}
