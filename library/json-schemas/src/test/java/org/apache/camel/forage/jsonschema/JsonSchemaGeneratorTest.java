package org.apache.camel.forage.jsonschema;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class JsonSchemaGeneratorTest {

    @Test
    void shouldCreateJsonSchemaGenerator() {
        JsonSchemaGenerator generator = new JsonSchemaGenerator();
        assertThat(generator).isNotNull();
    }

    @Test
    void shouldCreateJsonSchemaService() {
        JsonSchemaService service = new JsonSchemaService();
        assertThat(service).isNotNull();
    }
}
