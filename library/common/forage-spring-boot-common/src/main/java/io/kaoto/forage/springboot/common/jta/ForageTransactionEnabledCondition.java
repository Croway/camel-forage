package io.kaoto.forage.springboot.common.jta;

import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.boot.autoconfigure.condition.ConditionMessage;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.type.AnnotatedTypeMetadata;
import io.kaoto.forage.core.util.config.ConfigModule;
import io.kaoto.forage.core.util.config.ConfigStore;

/**
 * Condition for {@link ConditionalOnAnyForageTransactionEnabled} that matches when any
 * property matching {@code forage(\.<prefix>)?.<modulePrefix>.transaction.enabled}
 * has value {@code "true"}.
 *
 * <p>Property names are checked in three places:
 * <ol>
 *   <li>All enumerable Spring Environment property sources (dotted keys, e.g.,
 *       {@code forage.ds1.jdbc.transaction.enabled} from application.properties)</li>
 *   <li>Environment-variable-style keys (e.g., {@code FORAGE_JDBC_TRANSACTION_ENABLED} from
 *       {@code SystemEnvironmentPropertySource}), which are translated to the dotted lowercase
 *       form before matching — preserving the relaxed binding behavior of the previous
 *       {@code @ConditionalOnProperty}-based configuration</li>
 *   <li>Forage's {@link ConfigStore}, which covers {@code forage-*.properties} files loaded
 *       only into Forage's config system and not into the Spring Environment</li>
 * </ol>
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
                    if (pattern.matcher(toDottedForm(name)).matches()) {
                        String value = String.valueOf(eps.getProperty(name));
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

        // Fall back to Forage's ConfigStore, which covers forage-*.properties files that are
        // loaded only into Forage's config system (not into the Spring Environment) — the same
        // source consulted by prefix discovery in ForageSpringBootModuleAdapter.
        for (Map.Entry<Object, Object> entry : ConfigStore.getInstance().entries()) {
            String name = entry.getKey() instanceof ConfigModule configModule
                    ? configModule.propertyName()
                    : String.valueOf(entry.getKey());
            if (name != null
                    && pattern.matcher(name).matches()
                    && "true".equalsIgnoreCase(String.valueOf(entry.getValue()))) {
                return ConditionOutcome.match(
                        ConditionMessage.forCondition(ConditionalOnAnyForageTransactionEnabled.class)
                                .found("ConfigStore property")
                                .items(ConditionMessage.Style.QUOTE, name));
            }
        }

        return ConditionOutcome.noMatch(ConditionMessage.forCondition(ConditionalOnAnyForageTransactionEnabled.class)
                .didNotFind("any property matching forage[.<prefix>]." + modulePrefix + ".transaction.enabled=true")
                .atAll());
    }

    /**
     * Translates environment-variable-style names (e.g., {@code FORAGE_JDBC_TRANSACTION_ENABLED})
     * to the dotted lowercase form ({@code forage.jdbc.transaction.enabled}) so they can be
     * matched against the property regex. Dotted names are returned unchanged.
     */
    private static String toDottedForm(String name) {
        if (name.indexOf('_') >= 0 && name.indexOf('.') < 0) {
            return name.toLowerCase().replace('_', '.');
        }
        return name;
    }
}
