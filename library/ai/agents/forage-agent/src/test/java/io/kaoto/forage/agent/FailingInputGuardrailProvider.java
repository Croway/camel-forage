package io.kaoto.forage.agent;

import io.kaoto.forage.core.annotations.ForageBean;
import io.kaoto.forage.core.guardrails.InputGuardrailProvider;
import dev.langchain4j.guardrail.InputGuardrail;

@ForageBean(value = FailingInputGuardrailProvider.NAME, description = "Always-failing test input guardrail")
public class FailingInputGuardrailProvider implements InputGuardrailProvider {

    public static final String NAME = "test-failing-input";

    @Override
    public InputGuardrail create(String id) {
        throw new RuntimeException("Simulated guardrail creation failure");
    }
}
