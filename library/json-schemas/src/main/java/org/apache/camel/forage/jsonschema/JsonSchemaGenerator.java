package org.apache.camel.forage.jsonschema;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.victools.jsonschema.generator.Option;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfig;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.SchemaVersion;
import com.github.victools.jsonschema.module.jackson.JacksonModule;
import java.util.ArrayList;
import java.util.List;
import org.apache.camel.forage.jsonschema.model.ObjectInfo;
import org.apache.camel.forage.jsonschema.model.ObjectSchema;
import org.apache.camel.forage.jsonschema.model.SchemaRequest;
import org.apache.camel.forage.jsonschema.model.SchemaResponse;
import org.apache.camel.main.download.DependencyDownloaderClassLoader;
import org.apache.camel.main.download.MavenDependencyDownloader;
import org.apache.camel.tooling.maven.MavenArtifact;

public class JsonSchemaGenerator {

    private final SchemaGenerator schemaGenerator;

    public JsonSchemaGenerator() {
        this.schemaGenerator = createSchemaGenerator();
    }

    public SchemaResponse generateSchemas(SchemaRequest request) throws Exception {
        DependencyDownloaderClassLoader classLoader = createClassLoader(request);

        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(classLoader);

            List<ObjectSchema> schemas = new ArrayList<>();

            for (ObjectInfo objectInfo : request.getObjects()) {
                try {
                    Class<?> targetClass = classLoader.loadClass(objectInfo.getFullyQualifiedName());
                    JsonNode schema = schemaGenerator.generateSchema(targetClass);
                    schemas.add(
                            new ObjectSchema(objectInfo.getFullyQualifiedName(), objectInfo.getDescription(), schema));
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException("Class not found: " + objectInfo.getFullyQualifiedName(), e);
                } catch (Exception e) {
                    throw new RuntimeException(
                            "Failed to generate schema for class: " + objectInfo.getFullyQualifiedName(), e);
                }
            }

            return new SchemaResponse(request.getGroupId(), request.getArtifactId(), request.getVersion(), schemas);

        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
    }

    private SchemaGenerator createSchemaGenerator() {
        JacksonModule jacksonModule = new JacksonModule();
        SchemaGeneratorConfig config = new SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_2020_12)
                .with(jacksonModule)
                .with(Option.FLATTENED_ENUMS_FROM_TOSTRING)
                .without(Option.FIELDS_DERIVED_FROM_ARGUMENTFREE_METHODS)
                .without(Option.NONSTATIC_NONVOID_NONGETTER_METHODS)
                .without(Option.NULLABLE_METHOD_RETURN_VALUES_BY_DEFAULT)
                .without(Option.VOID_METHODS)
                .without(Option.GETTER_METHODS)
                .without(Option.FIELDS_DERIVED_FROM_ARGUMENTFREE_METHODS)
                .without(Option.STATIC_METHODS)
                .build();

        return new SchemaGenerator(config);
    }

    private DependencyDownloaderClassLoader createClassLoader(SchemaRequest request) throws Exception {
        DependencyDownloaderClassLoader classLoader =
                new DependencyDownloaderClassLoader(JsonSchemaGenerator.class.getClassLoader());

        MavenDependencyDownloader downloader = new MavenDependencyDownloader();
        downloader.setClassLoader(classLoader);
        downloader.setDownload(true);
        downloader.setRepositories(null);
        downloader.start();

        List<MavenArtifact> artifacts =
                downloader.downloadArtifacts(request.getGroupId(), request.getArtifactId(), request.getVersion(), true);

        if (artifacts == null || artifacts.isEmpty()) {
            throw new RuntimeException("No artifacts found for " + request.getGroupId() + ":" + request.getArtifactId()
                    + ":" + request.getVersion());
        }

        artifacts.forEach(artifact -> classLoader.addFile(artifact.getFile()));
        return classLoader;
    }
}
