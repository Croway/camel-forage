package io.kaoto.forage.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import org.apache.camel.CamelContext;
import org.apache.camel.CamelContextAware;
import org.apache.camel.support.service.ServiceSupport;
import org.apache.camel.util.FileUtil;
import org.apache.camel.util.IOHelper;
import org.apache.camel.util.ObjectHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.kaoto.forage.core.common.BeanFactory;
import io.kaoto.forage.core.util.config.ConfigStore;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

/**
 * File watcher that monitors the working directory for changes to {@code .properties} files
 * containing Forage configuration ({@code forage.*} keys) and triggers bean hot-reload.
 *
 * <p>This class mirrors the architecture of Camel's {@code FileWatcherResourceReloadStrategy}:
 * <ul>
 *   <li>Extends {@link ServiceSupport} for proper lifecycle management</li>
 *   <li>Uses CamelContext's {@code ExecutorServiceManager} for the background thread</li>
 *   <li>Uses Java NIO {@link WatchService} with macOS workaround</li>
 *   <li>Polls with a configurable timeout (default 2 seconds)</li>
 * </ul>
 *
 * <p>When a property file change is detected, the reload cycle is:
 * <ol>
 *   <li><strong>Cleanup</strong> — call {@link BeanFactory#cleanup()} on all factories
 *       (old config is still in ConfigStore for bean name resolution)</li>
 *   <li><strong>Clear config</strong> — call {@link ConfigStore#reload()} to clear cached values</li>
 *   <li><strong>Reconfigure</strong> — call {@link BeanFactory#configure()} on all factories
 *       (re-reads properties from disk, creates new beans, binds to registry)</li>
 * </ol>
 *
 * @since 1.1
 */
public class ForageReloadWatcher extends ServiceSupport implements CamelContextAware {

    private static final Logger LOG = LoggerFactory.getLogger(ForageReloadWatcher.class);

    private final List<BeanFactory> beanFactories;
    private CamelContext camelContext;
    private WatchService watcher;
    private ExecutorService executorService;
    private WatchFileChangesTask task;
    private long pollTimeout = 2000;

    public ForageReloadWatcher(List<BeanFactory> beanFactories) {
        this.beanFactories = beanFactories;
    }

    public void setPollTimeout(long pollTimeout) {
        this.pollTimeout = pollTimeout;
    }

    @Override
    public CamelContext getCamelContext() {
        return camelContext;
    }

    @Override
    public void setCamelContext(CamelContext camelContext) {
        this.camelContext = camelContext;
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();

        File dir = new File(".").getAbsoluteFile();
        if (!dir.exists() || !dir.isDirectory()) {
            LOG.warn("Working directory does not exist, Forage hot-reload disabled");
            return;
        }

        LOG.info("Forage hot-reload enabled: watching directory {} for property file changes", dir);

        WatchEvent.Modifier modifier = null;

        // macOS workaround: enable SensitivityWatchEventModifier.HIGH for faster polling
        // (same pattern as Camel's FileWatcherResourceReloadStrategy)
        String os = ObjectHelper.getSystemProperty("os.name", "");
        if (os.toLowerCase(Locale.US).startsWith("mac")) {
            Class<WatchEvent.Modifier> clazz = camelContext
                    .getClassResolver()
                    .resolveClass("com.sun.nio.file.SensitivityWatchEventModifier", WatchEvent.Modifier.class);
            if (clazz != null) {
                WatchEvent.Modifier[] modifiers = clazz.getEnumConstants();
                for (WatchEvent.Modifier mod : modifiers) {
                    if ("HIGH".equals(mod.name())) {
                        modifier = mod;
                        break;
                    }
                }
            }
            if (modifier != null) {
                LOG.debug(
                        "On Mac OS X the JDK WatchService is slow by default so enabling SensitivityWatchEventModifier.HIGH as workaround");
            } else {
                LOG.warn(
                        "On Mac OS X the JDK WatchService is slow and it may take up till 10 seconds to notice file changes");
            }
        }

        try {
            Path path = dir.toPath();
            watcher = path.getFileSystem().newWatchService();
            if (modifier != null) {
                path.register(watcher, new WatchEvent.Kind<?>[] {ENTRY_CREATE, ENTRY_MODIFY}, modifier);
            } else {
                path.register(watcher, ENTRY_CREATE, ENTRY_MODIFY);
            }

            task = new WatchFileChangesTask(watcher, path);

            executorService =
                    camelContext.getExecutorServiceManager().newSingleThreadExecutor(this, "ForageReloadWatcher");
            executorService.submit(task);
        } catch (IOException e) {
            throw new RuntimeException("Failed to start Forage file watcher", e);
        }
    }

    @Override
    protected void doStop() throws Exception {
        super.doStop();

        if (executorService != null) {
            camelContext.getExecutorServiceManager().shutdown(executorService);
            executorService = null;
        }

        if (watcher != null) {
            IOHelper.close(watcher);
        }
    }

    private void reloadBeans() {
        LOG.info("Forage property change detected, reloading beans...");

        // Phase 1: cleanup (old config still in ConfigStore for bean name resolution)
        // Note: AutoCloseable resources (e.g., DataSource pools) are NOT closed here
        // because Camel components cache references at the component level (e.g., SqlComponent.dataSource).
        // Closing the pool would break in-flight requests and cached references.
        // The old resources are unbound and will be GC'd after route reload.
        for (BeanFactory factory : beanFactories) {
            try {
                factory.cleanup();
            } catch (Exception e) {
                LOG.warn(
                        "Failed to cleanup bean factory: {}", factory.getClass().getName(), e);
            }
        }

        // Phase 2: clear config cache
        LOG.debug("Clearing ConfigStore and ConfigHelper caches");
        ConfigStore.getInstance().reload();

        // Phase 3: reconfigure with fresh values from disk
        for (BeanFactory factory : beanFactories) {
            try {
                factory.configure();
                LOG.info("Reloaded bean factory: {}", factory.getClass().getName());
            } catch (Exception e) {
                LOG.warn("Failed to reload bean factory: {}", factory.getClass().getName(), e);
            }
        }

        // Phase 4: remove Camel components so they are recreated from scratch on next
        // route start. Components cache bean references in different ways:
        // - SqlComponent caches DataSource via @Metadata(autowired)
        // - JmsComponent caches ConnectionFactory via JmsConfiguration
        // Stop/start doesn't clear these — only removing forces a fresh instance.
        removeComponents();

        // Phase 5: reload routes so they pick up the new bean/component references.
        // Camel's RouteWatcherReloadStrategy may also detect the .properties change and
        // reload routes — in that case a harmless "duplicate route id" warning is logged.
        // But we must reload here because Camel's watcher may not watch .properties files
        // (depends on the route file pattern passed to the run command).
        try {
            LOG.info("Reloading routes to pick up new bean references...");
            camelContext.getRouteController().reloadAllRoutes();
        } catch (Exception e) {
            LOG.warn("Failed to reload routes after bean refresh: {}", e.getMessage(), e);
        }
    }

    /**
     * Removes all Camel components so they are recreated from scratch when routes
     * are restarted by Camel's RouteWatcherReloadStrategy. This ensures components
     * pick up new bean references (DataSource, ConnectionFactory, etc.) from the registry.
     */
    private void removeComponents() {
        // Copy the list to avoid ConcurrentModificationException
        for (String name : new java.util.ArrayList<>(camelContext.getComponentNames())) {
            try {
                camelContext.removeComponent(name);
                LOG.debug("Removed component '{}' for re-creation", name);
            } catch (Exception e) {
                LOG.debug("Could not remove component '{}': {}", name, e.getMessage());
            }
        }
    }

    /**
     * Checks whether a properties file contains any Forage configuration keys.
     */
    private static boolean containsForageProperties(File file) {
        try (FileInputStream fis = new FileInputStream(file)) {
            Properties props = new Properties();
            props.load(fis);
            return props.keySet().stream().anyMatch(key -> key.toString().startsWith("forage."));
        } catch (IOException e) {
            LOG.debug("Could not read properties file {}: {}", file, e.getMessage());
            return false;
        }
    }

    /**
     * Background task that polls for property file changes.
     * Mirrors Camel's {@code WatchFileChangesTask} pattern.
     */
    private class WatchFileChangesTask implements Runnable {

        private final WatchService watcher;
        private final Path folder;
        private volatile boolean running;

        WatchFileChangesTask(WatchService watcher, Path folder) {
            this.watcher = watcher;
            this.folder = folder;
        }

        public boolean isRunning() {
            return running;
        }

        @Override
        public void run() {
            LOG.debug("ForageReloadWatcher is starting to watch folder: {}", folder);

            while (isStarting() || isRunAllowed()) {
                running = true;

                WatchKey key;
                try {
                    key = watcher.poll(pollTimeout, TimeUnit.MILLISECONDS);
                } catch (InterruptedException ex) {
                    LOG.info("ForageReloadWatcher interrupted");
                    Thread.currentThread().interrupt();
                    break;
                }

                if (key != null) {
                    boolean shouldReload = false;

                    for (WatchEvent<?> event : key.pollEvents()) {
                        @SuppressWarnings("unchecked")
                        WatchEvent<Path> we = (WatchEvent<Path>) event;
                        Path path = we.context();
                        File file = folder.resolve(path).toFile();

                        if (file.isDirectory()) {
                            continue;
                        }

                        String name = FileUtil.compactPath(file.getPath());
                        if (name.endsWith(".properties")) {
                            LOG.debug("Detected modified properties file: {}", name);
                            if (containsForageProperties(file)) {
                                LOG.debug("File contains forage properties: {}", name);
                                shouldReload = true;
                            }
                        }
                    }

                    // Batch: reload once even if multiple files changed simultaneously
                    if (shouldReload) {
                        try {
                            reloadBeans();
                        } catch (Exception e) {
                            LOG.warn("Error during Forage bean reload: {}", e.getMessage(), e);
                        }
                    }

                    boolean valid = key.reset();
                    if (!valid) {
                        break;
                    }
                }
            }

            running = false;
            LOG.debug("ForageReloadWatcher stopped watching folder: {}", folder);
        }
    }
}
