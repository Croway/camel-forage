package io.kaoto.forage.core.util.config;

import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ConfigEntriesTest {

    private static class TestConfig implements Config {
        @Override
        public String name() {
            return "test-config-entries";
        }

        @Override
        public void register(String name, String value) {
            // NO-OP
        }
    }

    static final class TestConfigEntries extends ConfigEntries {
        static final ConfigModule URL = ConfigModule.of(TestConfig.class, "forage.entriestest.url");
        static final ConfigModule USER = ConfigModule.of(TestConfig.class, "forage.entriestest.user");

        static {
            initModules(TestConfigEntries.class, URL, USER);
        }
    }

    @Test
    void getModulesInitializesSubclassFromClassLiteral() {
        Map<ConfigModule, ConfigEntry> modules = ConfigEntries.getModules(TestConfigEntries.class);
        assertThat(modules.keySet()).contains(TestConfigEntries.URL, TestConfigEntries.USER);
    }

    @Test
    void findMatchesBaseModule() {
        Map<ConfigModule, ConfigEntry> modules = ConfigEntries.getModules(TestConfigEntries.class);
        Optional<ConfigModule> found = ConfigEntries.find(modules, null, "forage.entriestest.url");
        assertThat(found).contains(TestConfigEntries.URL);
    }

    @Test
    void findMatchesPrefixedNameThroughPrefixParameter() {
        // The prefixed module was never registered via registerPrefix, but find()
        // must still resolve the prefixed property name using the prefix parameter
        Map<ConfigModule, ConfigEntry> modules = ConfigEntries.getModules(TestConfigEntries.class);
        Optional<ConfigModule> found = ConfigEntries.find(modules, "myds", "forage.myds.entriestest.url");
        assertThat(found).isPresent();
        assertThat(found.get().name()).isIn("forage.entriestest.url", "forage.myds.entriestest.url");
    }

    @Test
    void registerPrefixAddsNamedVariants() {
        ConfigEntries.registerPrefix(TestConfigEntries.class, "ds9");
        Map<ConfigModule, ConfigEntry> modules = ConfigEntries.getModules(TestConfigEntries.class);
        assertThat(modules.keySet().stream().anyMatch(m -> m.match("forage.ds9.entriestest.url")))
                .isTrue();
    }
}
