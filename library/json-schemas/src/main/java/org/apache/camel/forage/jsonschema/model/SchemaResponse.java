package org.apache.camel.forage.jsonschema.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class SchemaResponse {

    @JsonProperty("groupId")
    private String groupId;

    @JsonProperty("artifactId")
    private String artifactId;

    @JsonProperty("version")
    private String version;

    @JsonProperty("schemas")
    private List<ObjectSchema> schemas;

    public SchemaResponse() {}

    public SchemaResponse(String groupId, String artifactId, String version, List<ObjectSchema> schemas) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.schemas = schemas;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public List<ObjectSchema> getSchemas() {
        return schemas;
    }

    public void setSchemas(List<ObjectSchema> schemas) {
        this.schemas = schemas;
    }
}
