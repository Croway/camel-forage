package io.kaoto.forage.agent;

import java.util.LinkedHashMap;
import java.util.Map;
import io.kaoto.forage.core.ai.EmbeddingModelProvider;
import io.kaoto.forage.core.annotations.ForageBean;
import dev.langchain4j.model.embedding.EmbeddingModel;

/**
 * Test-only {@link EmbeddingModelProvider} registered via {@code META-INF/services} so that
 * {@code AgentCreatorTest} can exercise the real {@code AgentCreator#createEmbeddingModel} bridge:
 * at creation time it captures every {@code forage.test.embed.*} system property (the keys the
 * bridge is expected to set for the {@code test-embed} kind) into a static holder.
 */
@ForageBean(value = CapturingEmbeddingModelProvider.KIND, description = "Test-only capturing embedding model provider")
public class CapturingEmbeddingModelProvider implements EmbeddingModelProvider {

    public static final String KIND = "test-embed";
    public static final String PROPERTY_PREFIX = "forage.test.embed.";

    private static final Map<String, String> CAPTURED = new LinkedHashMap<>();
    private static String capturedId;

    @Override
    public EmbeddingModel create(String id) {
        capturedId = id;
        CAPTURED.clear();
        System.getProperties().stringPropertyNames().stream()
                .filter(name -> name.startsWith(PROPERTY_PREFIX))
                .forEach(name -> CAPTURED.put(name, System.getProperty(name)));
        // No real model is needed: tests only assert on the captured properties
        return null;
    }

    public static Map<String, String> captured() {
        return new LinkedHashMap<>(CAPTURED);
    }

    public static String capturedId() {
        return capturedId;
    }

    public static void reset() {
        CAPTURED.clear();
        capturedId = null;
    }
}
