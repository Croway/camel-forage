package io.kaoto.forage.agent;

import io.kaoto.forage.core.annotations.ForageBean;
import io.kaoto.forage.core.guardrails.InputGuardrailProvider;
import dev.langchain4j.guardrail.InputGuardrail;

@ForageBean(value = TestInputGuardrailProvider.NAME, description = "Test input guardrail")
public class TestInputGuardrailProvider implements InputGuardrailProvider {

    public static final String NAME = "test-input";
    private static String lastCreatedPrefix;

    @Override
    public InputGuardrail create(String id) {
        lastCreatedPrefix = id;
        return new NoopInputGuardrail();
    }

    public static String lastCreatedPrefix() {
        return lastCreatedPrefix;
    }

    public static void reset() {
        lastCreatedPrefix = null;
    }
}
