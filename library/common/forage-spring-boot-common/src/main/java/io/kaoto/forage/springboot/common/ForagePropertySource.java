package io.kaoto.forage.springboot.common;

import org.springframework.core.env.EnumerablePropertySource;
import io.kaoto.forage.core.util.config.ConfigStore;

/**
 * Spring {@link EnumerablePropertySource} that exposes Forage's {@link ConfigStore} properties
 * to Spring's {@link org.springframework.core.env.Environment}.
 *
 * <p>This enables standard Spring features like {@code @Value("${forage.jdbc.url}")},
 * {@code @ConditionalOnProperty(prefix = "forage.jdbc")}, and property resolution
 * in YAML/properties files to work with Forage configuration.
 *
 * <p>Properties are exposed using their dot-notation property names (e.g., "forage.jdbc.url")
 * as they appear in ConfigModule definitions.
 *
 * @since 1.1
 */
public class ForagePropertySource extends EnumerablePropertySource<ConfigStore> {

    public static final String FORAGE_PROPERTY_SOURCE_NAME = "forage";

    public ForagePropertySource() {
        super(FORAGE_PROPERTY_SOURCE_NAME, ConfigStore.getInstance());
    }

    @Override
    public String[] getPropertyNames() {
        return getSource().propertyNames().toArray(String[]::new);
    }

    @Override
    public Object getProperty(String name) {
        return getSource().getByPropertyName(name).orElse(null);
    }
}
