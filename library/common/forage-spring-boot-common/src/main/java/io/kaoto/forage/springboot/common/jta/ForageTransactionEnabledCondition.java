package io.kaoto.forage.springboot.common.jta;

import java.util.regex.Pattern;
import org.springframework.boot.autoconfigure.condition.ConditionMessage;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Condition for {@link ConditionalOnAnyForageTransactionEnabled} that matches when any
 * Spring Environment property matching {@code forage(\.<prefix>)?.<modulePrefix>.transaction.enabled}
 * has value {@code "true"}.
 *
 * <p>This handles both the default (unprefixed) key and named (prefixed) keys such as
 * {@code forage.ds1.jdbc.transaction.enabled}, so that named-config transactions correctly
 * activate the JTA transaction manager on Spring Boot.
 *
 * @since 1.2
 */
public class ForageTransactionEnabledCondition extends SpringBootCondition {

    @Override
    public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
        AnnotationAttributes attributes = AnnotationAttributes.fromMap(
                metadata.getAnnotationAttributes(ConditionalOnAnyForageTransactionEnabled.class.getName()));

        if (attributes == null) {
            return ConditionOutcome.noMatch(
                    ConditionMessage.forCondition(ConditionalOnAnyForageTransactionEnabled.class)
                            .because("annotation not found"));
        }

        String modulePrefix = attributes.getString("modulePrefix");
        String regex = "forage(\\..+)?\\." + Pattern.quote(modulePrefix) + "\\.transaction\\.enabled";
        Pattern pattern = Pattern.compile(regex);

        if (!(context.getEnvironment() instanceof ConfigurableEnvironment configurableEnv)) {
            return ConditionOutcome.noMatch(
                    ConditionMessage.forCondition(ConditionalOnAnyForageTransactionEnabled.class)
                            .because("environment is not configurable"));
        }

        for (var ps : configurableEnv.getPropertySources()) {
            if (ps instanceof EnumerablePropertySource<?> eps) {
                for (String name : eps.getPropertyNames()) {
                    if (pattern.matcher(name).matches()) {
                        String value = configurableEnv.getProperty(name);
                        if ("true".equalsIgnoreCase(value)) {
                            return ConditionOutcome.match(
                                    ConditionMessage.forCondition(ConditionalOnAnyForageTransactionEnabled.class)
                                            .found("property")
                                            .items(ConditionMessage.Style.QUOTE, name));
                        }
                    }
                }
            }
        }

        return ConditionOutcome.noMatch(ConditionMessage.forCondition(ConditionalOnAnyForageTransactionEnabled.class)
                .didNotFind("any property matching forage[.<prefix>]." + modulePrefix + ".transaction.enabled=true")
                .atAll());
    }
}
