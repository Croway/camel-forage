package org.apache.camel.forage.jsonschema.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

public class ObjectSchema {

    @JsonProperty("fullyQualifiedName")
    private String fullyQualifiedName;

    @JsonProperty("description")
    private String description;

    @JsonProperty("schema")
    private JsonNode schema;

    public ObjectSchema() {}

    public ObjectSchema(String fullyQualifiedName, String description, JsonNode schema) {
        this.fullyQualifiedName = fullyQualifiedName;
        this.description = description;
        this.schema = schema;
    }

    public String getFullyQualifiedName() {
        return fullyQualifiedName;
    }

    public void setFullyQualifiedName(String fullyQualifiedName) {
        this.fullyQualifiedName = fullyQualifiedName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public JsonNode getSchema() {
        return schema;
    }

    public void setSchema(JsonNode schema) {
        this.schema = schema;
    }
}
