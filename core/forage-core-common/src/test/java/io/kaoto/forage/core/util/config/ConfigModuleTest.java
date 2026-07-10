package io.kaoto.forage.core.util.config;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigModuleTest {

    private class TestConfig implements Config {

        @Override
        public String name() {
            return "test-config";
        }

        @Override
        public void register(String name, String value) {
            // NO-OP
        }
    }

    @Test
    void asNamed() {
        final ConfigModule configModule = ConfigModule.of(TestConfig.class, "test.config");
        assertEquals("test.config", configModule.name());

        final ConfigModule configModulePrefixed = configModule.asNamed("prefix");
        assertEquals("prefix.test.config", configModulePrefixed.name());

        final ConfigModule configModulePrefixed2 = configModulePrefixed.asNamed("prefix2");
        assertEquals("prefix2.test.config", configModulePrefixed2.name());
    }

    @Test
    void asNamedPreservesMetadata() {
        final ConfigModule configModule = ConfigModule.of(
                TestConfig.class,
                "forage.test.config",
                "A description",
                "A Label",
                "def",
                "string",
                true,
                ConfigTag.COMMON);

        final ConfigModule named = configModule.asNamed("prefix");
        assertEquals("forage.prefix.test.config", named.name());
        assertEquals("A description", named.description());
        assertEquals("A Label", named.label());
        assertEquals("def", named.defaultValue());
        assertEquals("string", named.type());
        assertTrue(named.required());
        assertEquals(ConfigTag.COMMON, named.configTag());
    }

    @Test
    void propertyNameKeepsUnderscores() {
        final ConfigModule configModule = ConfigModule.of(TestConfig.class, "forage.jdbc.url");
        final ConfigModule named = configModule.asNamed("my_datasource");
        assertEquals("forage.my_datasource.jdbc.url", named.propertyName());
    }

    @Test
    void envNameConvertsDotsAndCase() {
        final ConfigModule configModule = ConfigModule.of(TestConfig.class, "forage.jdbc.url");
        assertEquals("FORAGE_JDBC_URL", configModule.envName());
    }

    @Test
    void match() {
        final ConfigModule configModule = ConfigModule.of(TestConfig.class, "test.config");
        assertTrue(configModule.match("test.config"));

        final ConfigModule configModulePrefixed = configModule.asNamed("prefix");
        assertFalse(configModulePrefixed.match("test.config"));
        assertTrue(configModulePrefixed.match("prefix.test.config"));

        final ConfigModule configModulePrefixed2 = configModulePrefixed.asNamed("prefix2");
        assertFalse(configModulePrefixed2.match("test.config"));
        assertTrue(configModulePrefixed2.match("prefix2.test.config"));
    }
}
