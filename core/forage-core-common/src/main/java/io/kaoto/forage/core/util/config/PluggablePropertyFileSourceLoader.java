package io.kaoto.forage.core.util.config;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.WeakHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Discovers {@link PluggablePropertyFileSource} implementations using the Java
 * {@link ServiceLoader} mechanism.
 *
 * <p>Discovered sources are sorted by {@link PropertyFileSource#priority()} in descending
 * order (highest priority first) and cached per classloader, so environments with multiple
 * or short-lived classloaders (Quarkus augmentation, JBang, application servers) each see
 * the sources visible to their own context classloader instead of whichever classloader
 * happened to trigger the first load.
 *
 * @since 1.2
 * @see PluggablePropertyFileSource
 */
public final class PluggablePropertyFileSourceLoader {

    private static final Logger LOG = LoggerFactory.getLogger(PluggablePropertyFileSourceLoader.class);

    /**
     * Cache keyed by classloader; weak keys so discarded classloaders (and their sources)
     * can be collected.
     */
    private static final Map<ClassLoader, List<PluggablePropertyFileSource>> CACHE = new WeakHashMap<>();

    private PluggablePropertyFileSourceLoader() {}

    /**
     * Returns the discovered pluggable property file sources, sorted by descending priority.
     *
     * <p>The result is cached per classloader after the first call. Use {@link #reload()}
     * to re-discover sources.
     *
     * @return an unmodifiable list of pluggable sources
     */
    public static List<PluggablePropertyFileSource> getSources() {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader == null) {
            classLoader = PluggablePropertyFileSourceLoader.class.getClassLoader();
        }
        synchronized (CACHE) {
            return CACHE.computeIfAbsent(classLoader, PluggablePropertyFileSourceLoader::loadSources);
        }
    }

    /**
     * Forces re-discovery of pluggable property file sources on the next call to {@link #getSources()}.
     */
    public static void reload() {
        synchronized (CACHE) {
            CACHE.clear();
        }
    }

    private static List<PluggablePropertyFileSource> loadSources(ClassLoader classLoader) {
        List<PluggablePropertyFileSource> sources = new ArrayList<>();
        ServiceLoader<PluggablePropertyFileSource> loader =
                ServiceLoader.load(PluggablePropertyFileSource.class, classLoader);
        for (PluggablePropertyFileSource source : loader) {
            LOG.info("Discovered pluggable property file source: {} (priority={})", source.name(), source.priority());
            sources.add(source);
        }
        sources.sort(Comparator.comparingInt(PropertyFileSource::priority).reversed());
        return List.copyOf(sources);
    }
}
