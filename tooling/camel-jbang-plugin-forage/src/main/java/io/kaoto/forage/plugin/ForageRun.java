package io.kaoto.forage.plugin;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import org.apache.camel.dsl.jbang.core.commands.CamelJBangMain;
import org.apache.camel.dsl.jbang.core.commands.Run;
import org.apache.camel.main.KameletMain;
import org.apache.camel.util.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.kaoto.forage.core.common.ExportCustomizer;
import io.kaoto.forage.core.common.RuntimeType;
import picocli.CommandLine;

public class ForageRun extends Run {

    private static final Logger LOG = LoggerFactory.getLogger(ForageRun.class);

    @CommandLine.Mixin
    private PropertyValidationMixin validation;

    public ForageRun(CamelJBangMain main) {
        super(main);
    }

    @Override
    public Integer doCall() throws Exception {
        resolveConfigDir();

        // Validate properties before running
        int validationResult = validation.validateAndReport(printer());
        if (validationResult != 0) {
            return validationResult;
        }

        return super.doCall();
    }

    /**
     * Derives the configuration directory from the file arguments, mirroring
     * how Camel's {@code Run} resolves its base directory.
     *
     * <ul>
     *   <li>Single directory argument (e.g., {@code camel forage run agent}) — use that directory</li>
     *   <li>Multiple files sharing a common parent different from CWD
     *       (e.g., shell-expanded {@code agent/*}) — use that common parent</li>
     *   <li>Otherwise — leave unset (CWD is fine)</li>
     * </ul>
     *
     * <p>Sets {@code forage.config.dir} system property so that {@code ConfigStore},
     * {@code ConfigHelper}, {@code ForagePropertyValidator}, and {@code ForageReloadWatcher}
     * all resolve properties from the correct location.
     */
    private void resolveConfigDir() {
        if (files == null || files.isEmpty()) {
            return;
        }

        Path configDir = null;

        // Single directory argument — same logic as Camel's Run (Run.java line 452-462)
        if (files.size() == 1) {
            String name = FileUtil.stripTrailingSeparator(files.get(0));
            Path first = Path.of(name);
            if (Files.isDirectory(first)) {
                configDir = first;
            }
        }

        // Multiple files (e.g., shell-expanded glob) — find common parent
        if (configDir == null && files.size() > 1) {
            configDir = commonParent(files);
        }

        if (configDir != null) {
            Path cwd = Path.of("").toAbsolutePath();
            Path resolved = configDir.toAbsolutePath().normalize();
            if (!resolved.equals(cwd)) {
                LOG.debug("Setting forage.config.dir={} (derived from file arguments)", resolved);
                System.setProperty("forage.config.dir", resolved.toString());
            }
        }
    }

    /**
     * Returns the common parent directory of the given file paths,
     * or {@code null} if there is no single common parent.
     */
    static Path commonParent(java.util.List<String> filePaths) {
        Path common = null;
        for (String filePath : filePaths) {
            Path parent = Path.of(filePath).toAbsolutePath().normalize().getParent();
            if (parent == null) {
                return null;
            }
            if (common == null) {
                common = parent;
            } else if (!common.equals(parent)) {
                return null;
            }
        }
        return common;
    }

    /**
     * Propagates the --dev flag as forage.reload.enabled system property so that
     * ForageContextServicePlugin can detect dev mode and start the file watcher.
     *
     * <p>This method is called after the --dev flag has been processed (Run.java line 592)
     * and camel.main.routesReloadEnabled has been added to initial properties.
     */
    @Override
    protected void doAddInitialProperty(KameletMain main) {
        if ("true".equals(main.getInitialProperties().getProperty("camel.main.routesReloadEnabled"))) {
            System.setProperty("forage.reload.enabled", "true");
        }
    }

    /**
     * This method is used only for the camel run command with runtime=main
     * All other runtimes extends dependencies by interface {@link org.apache.camel.dsl.jbang.core.common.PluginExporter}
     * from {@link io.kaoto.forage.plugin.ForagePlugin}
     */
    @Override
    protected void addDependencies(String... deps) {
        // gather dependencies across all (enabled) export customizers for the runtime `camel-main`
        var dependencies = ExportHelper.getAllCustomizers()
                .filter(ExportCustomizer::isEnabled)
                .map(exportCustomizer -> exportCustomizer.resolveRuntimeDependencies(RuntimeType.main))
                .flatMap(Set::stream)
                .distinct()
                .toArray(String[]::new);

        super.addDependencies(dependencies);
    }
}
