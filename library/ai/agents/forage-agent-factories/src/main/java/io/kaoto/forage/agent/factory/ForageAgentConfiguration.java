package io.kaoto.forage.agent.factory;

import java.util.ArrayList;
import java.util.List;
import org.apache.camel.component.langchain4j.agent.api.AgentConfiguration;
import dev.langchain4j.guardrail.InputGuardrail;
import dev.langchain4j.guardrail.OutputGuardrail;

/**
 * Extended agent configuration that supports guardrail instances in addition to classes.
 *
 * <p>This class extends the standard Camel {@link AgentConfiguration} to provide support
 * for pre-configured guardrail instances. When guardrail instances are provided, they
 * take precedence over guardrail classes, allowing for properly configured guardrails
 * loaded via ServiceLoader.
 */
public class ForageAgentConfiguration extends AgentConfiguration {

    private List<InputGuardrail> inputGuardrails;
    private List<OutputGuardrail> outputGuardrails;

    public ForageAgentConfiguration() {
        super();
    }

    public List<InputGuardrail> getInputGuardrails() {
        return inputGuardrails;
    }

    public ForageAgentConfiguration withInputGuardrails(List<InputGuardrail> inputGuardrails) {
        this.inputGuardrails = inputGuardrails;
        return this;
    }

    public ForageAgentConfiguration withInputGuardrail(InputGuardrail inputGuardrail) {
        if (this.inputGuardrails == null) {
            this.inputGuardrails = new ArrayList<>();
        }
        this.inputGuardrails.add(inputGuardrail);
        return this;
    }

    public List<OutputGuardrail> getOutputGuardrails() {
        return outputGuardrails;
    }

    public ForageAgentConfiguration withOutputGuardrails(List<OutputGuardrail> outputGuardrails) {
        this.outputGuardrails = outputGuardrails;
        return this;
    }

    public ForageAgentConfiguration withOutputGuardrail(OutputGuardrail outputGuardrail) {
        if (this.outputGuardrails == null) {
            this.outputGuardrails = new ArrayList<>();
        }
        this.outputGuardrails.add(outputGuardrail);
        return this;
    }

    public boolean hasInputGuardrails() {
        return inputGuardrails != null && !inputGuardrails.isEmpty();
    }

    public boolean hasOutputGuardrails() {
        return outputGuardrails != null && !outputGuardrails.isEmpty();
    }
}
