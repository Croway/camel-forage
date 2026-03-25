package io.kaoto.forage.plugin;

import java.util.Set;
import org.apache.camel.dsl.jbang.core.commands.CamelJBangMain;
import org.apache.camel.dsl.jbang.core.commands.Run;
import org.apache.camel.main.KameletMain;
import io.kaoto.forage.core.common.ExportCustomizer;
import io.kaoto.forage.core.common.RuntimeType;
import picocli.CommandLine;

public class ForageRun extends Run {

    @CommandLine.Mixin
    private PropertyValidationMixin validation;

    public ForageRun(CamelJBangMain main) {
        super(main);
    }

    @Override
    public Integer doCall() throws Exception {
        // Validate properties before running
        int validationResult = validation.validateAndReport(printer());
        if (validationResult != 0) {
            return validationResult;
        }

        return super.doCall();
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
