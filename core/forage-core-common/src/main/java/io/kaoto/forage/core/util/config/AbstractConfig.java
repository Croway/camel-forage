package io.kaoto.forage.core.util.config;

import java.util.Optional;

public abstract class AbstractConfig implements Config {

    private final String prefix;
    private final Class<? extends ConfigEntries> entriesClass;

    protected AbstractConfig(String prefix, Class<? extends ConfigEntries> entriesClass) {
        this.prefix = prefix;
        this.entriesClass = entriesClass;
        ConfigEntries.registerPrefix(entriesClass, prefix);
        loadFromProperties();
        ConfigEntries.loadOverridesFor(entriesClass, prefix);
    }

    @SuppressWarnings("unchecked")
    private <T extends Config> void loadFromProperties() {
        ConfigStore.getInstance().load((Class<T>) this.getClass(), (T) this, this::register);
    }

    protected String prefix() {
        return prefix;
    }

    @Override
    public void register(String name, String value) {
        ConfigEntries.find(ConfigEntries.getModules(entriesClass), prefix, name)
                .ifPresent(module -> ConfigStore.getInstance().set(module, PlaceholderResolver.resolve(value)));
    }

    protected Optional<String> get(ConfigModule module) {
        return ConfigStore.getInstance().get(module.asNamed(prefix));
    }

    protected String getRequired(ConfigModule module, String errorMessage) {
        return get(module).orElseThrow(() -> new MissingConfigException(errorMessage));
    }
}
