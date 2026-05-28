package io.kaoto.forage.core.util.config;

import java.util.Properties;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class PlaceholderResolverTest {

    @Test
    void resolveEnvAndSysPlaceholders() {
        String home = System.getenv("HOME");
        org.junit.jupiter.api.Assumptions.assumeTrue(home != null, "HOME env var must be set");
        String input = "user={{sys:user.name}},home={{env:HOME}}";
        String expected = "user=" + System.getProperty("user.name") + ",home=" + home;
        assertThat(PlaceholderResolver.resolve(input)).isEqualTo(expected);
    }

    @Test
    void resolveMissingWithDefault() {
        assertThat(PlaceholderResolver.resolve("{{env:FORAGE_TEST_NONEXISTENT_12345:fallback}}"))
                .isEqualTo("fallback");
    }

    @Test
    void resolveMissingWithoutDefaultLeavesUnchanged() {
        String input = "{{env:FORAGE_TEST_NONEXISTENT_12345}}";
        assertThat(PlaceholderResolver.resolve(input)).isEqualTo(input);
    }

    @Test
    void resolveAllOnProperties() {
        Properties props = new Properties();
        props.setProperty("resolved", "{{sys:user.name}}");
        props.setProperty("plain", "literal value");

        PlaceholderResolver.resolveAll(props);

        assertThat(props.getProperty("resolved")).isEqualTo(System.getProperty("user.name"));
        assertThat(props.getProperty("plain")).isEqualTo("literal value");
    }
}
