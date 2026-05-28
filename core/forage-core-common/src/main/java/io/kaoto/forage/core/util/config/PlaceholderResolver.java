package io.kaoto.forage.core.util.config;

import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.camel.util.IOHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Resolves Camel-style property placeholders ({@code {{env:KEY}}} and {@code {{sys:KEY}}})
 * in configuration values loaded from properties files.
 *
 * <p>Supported syntax:
 * <ul>
 *   <li>{@code {{env:KEY}}} — resolves to the environment variable {@code KEY}</li>
 *   <li>{@code {{env:KEY:default}}} — resolves to {@code KEY}, or {@code default} if not set</li>
 *   <li>{@code {{sys:KEY}}} — resolves to the system property {@code KEY}</li>
 *   <li>{@code {{sys:KEY:default}}} — resolves to {@code KEY}, or {@code default} if not set</li>
 * </ul>
 *
 * <p>Resolution is single-pass and non-recursive. Nested placeholders like
 * {@code {{env:KEY:{{sys:FALLBACK}}}}} are not supported.
 *
 * <p>If a placeholder references a variable that is not set and no default is provided,
 * the placeholder is left unchanged in the output.
 *
 * @since 1.4
 */
public final class PlaceholderResolver {

    private static final Logger LOG = LoggerFactory.getLogger(PlaceholderResolver.class);
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{\\{(env|sys):([^}:]+)(?::([^}]*))?}}");
    private static final String PLACEHOLDER_TOKEN = "{{";

    private PlaceholderResolver() {}

    /**
     * Returns {@code true} if the value contains at least one {@code {{env:...}}} or
     * {@code {{sys:...}}} placeholder.
     */
    public static boolean containsPlaceholders(String value) {
        if (value == null || !value.contains(PLACEHOLDER_TOKEN)) {
            return false;
        }
        return PLACEHOLDER_PATTERN.matcher(value).find();
    }

    /**
     * Resolves all {@code {{env:...}}} and {@code {{sys:...}}} placeholders in the given value.
     *
     * @param value the property value to resolve (may be {@code null})
     * @return the resolved value, or the original value if no placeholders are found
     */
    public static String resolve(String value) {
        if (value == null || !value.contains(PLACEHOLDER_TOKEN)) {
            return value;
        }

        Matcher matcher = PLACEHOLDER_PATTERN.matcher(value);
        StringBuilder sb = new StringBuilder();

        while (matcher.find()) {
            String function = matcher.group(1);
            String key = matcher.group(2);
            String defaultValue = matcher.group(3);

            String resolved = lookup(function, key);

            if (resolved != null) {
                matcher.appendReplacement(sb, Matcher.quoteReplacement(resolved));
            } else if (defaultValue != null) {
                matcher.appendReplacement(sb, Matcher.quoteReplacement(defaultValue));
            } else {
                LOG.debug("Placeholder {{{}:{}}} could not be resolved and has no default value", function, key);
            }
        }
        matcher.appendTail(sb);

        return sb.toString();
    }

    /**
     * Resolves all {@code {{env:...}}} and {@code {{sys:...}}} placeholders in the given
     * {@link Properties} object, modifying values in place.
     *
     * @param props the properties to process (may be {@code null})
     */
    public static void resolveAll(Properties props) {
        if (props == null || props.isEmpty()) {
            return;
        }

        for (String key : props.stringPropertyNames()) {
            String value = props.getProperty(key);
            if (value != null && value.contains(PLACEHOLDER_TOKEN)) {
                String resolved = resolve(value);
                if (!value.equals(resolved)) {
                    props.setProperty(key, resolved);
                }
            }
        }
    }

    private static String lookup(String function, String key) {
        if ("env".equals(function)) {
            return IOHelper.lookupEnvironmentVariable(key);
        }
        return System.getProperty(key);
    }
}
