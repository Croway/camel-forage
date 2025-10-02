package org.apache.camel.forage.jsonschema.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ObjectInfo {

    @JsonProperty("fullyQualifiedName")
    private String fullyQualifiedName;

    @JsonProperty("description")
    private String description;

    public ObjectInfo() {}

    public ObjectInfo(String fullyQualifiedName, String description) {
        this.fullyQualifiedName = fullyQualifiedName;
        this.description = description;
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
}
