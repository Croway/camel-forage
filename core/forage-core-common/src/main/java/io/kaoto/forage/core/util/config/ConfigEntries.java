package io.kaoto.forage.core.util.config;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ConfigEntries {

    private static final Logger LOG = LoggerFactory.getLogger(ConfigEntries.class);

    private static final Map<Class<? extends ConfigEntries>, Map<ConfigModule, ConfigEntry>> REGISTRY =
            new ConcurrentHashMap<>();

    private static final Map<Class<? extends ConfigEntries>, List<ConfigModule>> BASE_MODULES =
            new ConcurrentHashMap<>();

    protected static void initModules(Class<? extends ConfigEntries> clazz, ConfigModule... modules) {
        Map<ConfigModule, ConfigEntry> map = REGISTRY.computeIfAbsent(clazz, k -> new ConcurrentHashMap<>());
        for (ConfigModule module : modules) {
            map.put(module, ConfigEntry.fromModule());
        }
        BASE_MODULES.put(clazz, Arrays.asList(modules));
    }

    public static Map<ConfigModule, ConfigEntry> getModules(Class<? extends ConfigEntries> clazz) {
        ensureInitialized(clazz);
        return REGISTRY.computeIfAbsent(clazz, k -> new ConcurrentHashMap<>());
    }

    private static void ensureInitialized(Class<? extends ConfigEntries> clazz) {
        try {
            Class.forName(clazz.getName(), true, clazz.getClassLoader());
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Failed to initialize " + clazz.getName(), e);
        }
    }

    public static Map<ConfigModule, ConfigEntry> entriesOf(Class<? extends ConfigEntries> clazz) {
        return Collections.unmodifiableMap(getModules(clazz));
    }

    public static void registerPrefix(Class<? extends ConfigEntries> clazz, String prefix) {
        if (prefix != null) {
            Map<ConfigModule, ConfigEntry> modules = getModules(clazz);
            List<ConfigModule> base = BASE_MODULES.get(clazz);
            if (base != null) {
                for (ConfigModule module : base) {
                    modules.put(module.asNamed(prefix), ConfigEntry.fromModule());
                }
            } else {
                // ConfigEntries subclasses with dynamic modules (e.g., route policies)
                // manage their own registry instead of calling initModules()
                LOG.debug("registerPrefix: no base modules registered for {}", clazz.getName());
            }
        }
    }

    public static void loadOverridesFor(Class<? extends ConfigEntries> clazz, String prefix) {
        ensureInitialized(clazz);
        List<ConfigModule> base = BASE_MODULES.get(clazz);
        if (base != null) {
            for (ConfigModule module : base) {
                ConfigStore.getInstance().load(module.asNamed(prefix));
            }
        } else {
            LOG.debug("loadOverridesFor: no base modules registered for {}", clazz.getName());
        }
    }

    /**
     * Finds the module matching the given property name, either directly or as the
     * prefixed (named) variant of a base module.
     *
     * @param configModules the modules to search
     * @param prefix an optional configuration prefix; when set, a base module also matches
     *        its prefixed property name even if the prefix was never registered
     * @param name the full property name to match (e.g., {@code forage.ds1.jdbc.url})
     */
    public static Optional<ConfigModule> find(
            Map<ConfigModule, ConfigEntry> configModules, String prefix, String name) {
        return configModules.keySet().stream()
                .filter(m -> m.match(name) || m.asNamed(prefix).match(name))
                .findFirst();
    }

    /**
     * Tries loading the configuration represented by the set of configuration modules into the store
     * @param configModules the set of configuration modules to try loading into the configuration store
     * @param prefix an optional prefix for the configuration
     */
    public static void load(Map<ConfigModule, ConfigEntry> configModules, String prefix) {
        configModules.forEach((k, v) -> ConfigStore.getInstance().load(k.asNamed(prefix)));
    }
}
