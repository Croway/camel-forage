package org.apache.camel.forage.jsonschema;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.util.List;
import org.apache.camel.forage.jsonschema.model.SchemaRequest;
import org.apache.camel.forage.jsonschema.model.SchemaResponse;

public class JsonSchemaService {

    private final ObjectMapper objectMapper;
    private final JsonSchemaGenerator generator;

    public JsonSchemaService() {
        this.objectMapper = new ObjectMapper();
        this.generator = new JsonSchemaGenerator();
    }

    /**
     * Process a JSON file containing schema requests and generate schemas for all objects
     * @param inputFile JSON file containing list of SchemaRequest objects
     * @return List of SchemaResponse objects with generated schemas
     * @throws IOException if file cannot be read
     * @throws Exception if schema generation fails
     */
    public List<SchemaResponse> processSchemaRequests(File inputFile) throws Exception {
        SchemaRequest[] requests = objectMapper.readValue(inputFile, SchemaRequest[].class);

        return List.of(requests).stream()
                .map(request -> {
                    try {
                        return generator.generateSchemas(request);
                    } catch (Exception e) {
                        throw new RuntimeException(
                                "Failed to generate schemas for request: " + request.getArtifactId(), e);
                    }
                })
                .toList();
    }

    /**
     * Process a single schema request from JSON string
     * @param jsonRequest JSON string containing a SchemaRequest
     * @return SchemaResponse with generated schemas
     * @throws IOException if JSON cannot be parsed
     * @throws Exception if schema generation fails
     */
    public SchemaResponse processSchemaRequest(String jsonRequest) throws Exception {
        SchemaRequest request = objectMapper.readValue(jsonRequest, SchemaRequest.class);
        return generator.generateSchemas(request);
    }

    /**
     * Save schema responses to a JSON file
     * @param responses List of schema responses to save
     * @param outputFile File to write the responses to
     * @throws IOException if file cannot be written
     */
    public void saveSchemaResponses(List<SchemaResponse> responses, File outputFile) throws IOException {
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(outputFile, responses);
    }
}
