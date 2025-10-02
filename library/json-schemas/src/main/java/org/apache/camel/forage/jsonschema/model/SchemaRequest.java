package org.apache.camel.forage.jsonschema.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class SchemaRequest {

    @JsonProperty("groupId")
    private String groupId;

    @JsonProperty("artifactId")
    private String artifactId;

    @JsonProperty("version")
    private String version;

    @JsonProperty("objects")
    private List<ObjectInfo> objects;

    public SchemaRequest() {}

    public SchemaRequest(String groupId, String artifactId, String version, List<ObjectInfo> objects) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.objects = objects;
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

    public List<ObjectInfo> getObjects() {
        return objects;
    }

    public void setObjects(List<ObjectInfo> objects) {
        this.objects = objects;
    }
}
