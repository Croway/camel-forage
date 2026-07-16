package io.kaoto.forage.core.util.config;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class AbstractConfigTest {

    @Test
    void levenshteinComputesEditDistance() {
        assertThat(AbstractConfig.levenshtein("forage.jdbc.url", "forage.jdbc.url"))
                .isZero();
        assertThat(AbstractConfig.levenshtein("forage.jdbc.url", "forage.jdbc.uri"))
                .isEqualTo(1);
        assertThat(AbstractConfig.levenshtein("forage.jdbc.url", "forage.jbdc.url"))
                .isEqualTo(2);
        assertThat(AbstractConfig.levenshtein("", "abc")).isEqualTo(3);
    }
}
