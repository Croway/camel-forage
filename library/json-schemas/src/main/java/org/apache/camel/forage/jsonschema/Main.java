package org.apache.camel.forage.jsonschema;

import java.io.File;
import java.util.List;
import org.apache.camel.forage.jsonschema.model.SchemaResponse;

public class Main {

    public static void main(String[] args) throws Exception {
        JsonSchemaService service = new JsonSchemaService();

        // Input file from resources
        File inputFile = new File(
                "/Users/fmariani/Repositories/croway/camel-forage/library/json-schemas/src/main/resources/camel-components.json");
        //        File inputFile = new File("src/main/resources/camel-components.json");

        // Create target directory if it doesn't exist
        File targetDir = new File("target");
        if (!targetDir.exists()) {
            targetDir.mkdirs();
        }

        // Output file in target directory
        File outputFile = new File(targetDir, "camel-components-schemas.json");

        System.out.println("Processing input file: " + inputFile.getAbsolutePath());
        System.out.println("Generating schemas...");

        try {
            List<SchemaResponse> responses = service.processSchemaRequests(inputFile);
            service.saveSchemaResponses(responses, outputFile);

            System.out.println("Successfully generated schemas for " + responses.size() + " components");
            System.out.println("Output written to: " + outputFile.getAbsolutePath());

            // Print summary
            responses.forEach(response -> {
                System.out.println("- " + response.getArtifactId() + " ("
                        + response.getSchemas().size() + " schemas)");
            });

        } catch (Exception e) {
            System.err.println("Error processing schemas: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
