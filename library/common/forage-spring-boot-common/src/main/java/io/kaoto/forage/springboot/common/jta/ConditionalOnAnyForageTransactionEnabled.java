package io.kaoto.forage.springboot.common.jta;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.context.annotation.Conditional;

/**
 * {@link Conditional @Conditional} that matches when any Forage property of the form
 * {@code forage[.<prefix>].<modulePrefix>.transaction.enabled=true} is present in the
 * Spring Environment. This covers both the default (unprefixed) configuration and any
 * named (prefixed) configurations (e.g., {@code forage.ds1.jdbc.transaction.enabled}).
 *
 * @see ForageTransactionEnabledCondition
 * @since 1.2
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Conditional(ForageTransactionEnabledCondition.class)
public @interface ConditionalOnAnyForageTransactionEnabled {

    /**
     * The Forage module prefix (e.g., {@code "jdbc"} or {@code "jms"}).
     */
    String modulePrefix();
}
