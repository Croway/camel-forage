package io.kaoto.forage.plugin;

import org.apache.camel.dsl.jbang.core.commands.CamelJBangMain;
import org.apache.camel.dsl.jbang.core.commands.Run;
import picocli.CommandLine;

/**
 * Forage run command with property validation.
 *
 * <p>Extends the standard Camel JBang run command to resolve the Forage
 * configuration directory and validate Forage properties before running.
 *
 * <p>Users can run Forage applications via {@code camel forage run app/*}
 * (this subcommand) or via {@code camel run app/*} (the standard Camel run
 * command, where the Forage plugin hooks in via {@link ForagePlugin#getRunCustomizer()}).
 * Both paths use the same configuration directory resolution logic.
 *
 * @since 1.0
 */
public class ForageRun extends Run {

    @CommandLine.Mixin
    private PropertyValidationMixin validation;

    public ForageRun(CamelJBangMain main) {
        super(main);
    }

    @Override
    public Integer doCall() throws Exception {
        ForagePlugin.resolveConfigDir(files);

        // Validate properties before running
        int validationResult = validation.validateAndReport(printer());
        if (validationResult != 0) {
            return validationResult;
        }

        return super.doCall();
    }
}
