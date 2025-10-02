# JSON Schema Generator

This module provides JSON schema generation capabilities for Java objects based on Apache Camel dependency coordinates. It automatically downloads Maven dependencies and generates JSON schemas for specified Java classes.

## Overview

The JSON Schema Generator module allows you to:

- Generate JSON schemas for Java objects from Maven dependencies
- Process input JSON files containing dependency coordinates, class lists, and descriptions
- Output structured JSON responses with generated schemas
- Support for multiple objects per dependency
- Designed for use in Maven plugins for catalog generation

## Usage

### Input JSON Format

The module expects input JSON files with the following structure:

```json
[
  {
    "groupId": "org.apache.camel",
    "artifactId": "camel-fhir",
    "version": "4.14.0",
    "objects": [
      {
        "fullyQualifiedName": "org.hl7.fhir.r4.model.Patient",
        "description": "FHIR R4 Patient resource representing demographic and administrative information"
      },
      {
        "fullyQualifiedName": "org.hl7.fhir.r4.model.Observation",
        "description": "FHIR R4 Observation resource representing measurements and simple assertions"
      }
    ]
  }
]
```

### Output JSON Format

The module generates output JSON with the following structure:

```json
[
  {
    "groupId": "org.apache.camel",
    "artifactId": "camel-fhir",
    "version": "4.14.0",
    "schemas": [
      {
        "fullyQualifiedName": "org.hl7.fhir.r4.model.Patient",
        "description": "FHIR R4 Patient resource representing demographic and administrative information",
        "schema": {
          "$schema": "https://json-schema.org/draft/2020-12/schema",
          "type": "object",
          "properties": {
            // ... generated JSON schema ...
          }
        }
      }
    ]
  }
]
```

## Running the Generator

### Using the Main Class

The module includes a Main class that processes the included `camel-components.json` file:

```bash
# From the module directory
mvn exec:java -Dexec.mainClass="org.apache.camel.forage.jsonschema.Main"
```

This will:
1. Read `src/main/resources/camel-components.json`
2. Generate schemas for all specified objects
3. Output the results to `target/camel-components-schemas.json`

### Programmatic Usage

```java
import org.apache.camel.forage.jsonschema.JsonSchemaService;
import org.apache.camel.forage.jsonschema.model.SchemaResponse;

// Create service instance
JsonSchemaService service = new JsonSchemaService();

// Process from file
List<SchemaResponse> responses = service.processSchemaRequests(new File("input.json"));

// Save results
service.saveSchemaResponses(responses, new File("output.json"));
```

### Direct Generator Usage

```java
import org.apache.camel.forage.jsonschema.JsonSchemaGenerator;
import org.apache.camel.forage.jsonschema.model.*;

JsonSchemaGenerator generator = new JsonSchemaGenerator();

// Create request
List<ObjectInfo> objects = List.of(
    new ObjectInfo("org.hl7.fhir.r4.model.Patient", "FHIR Patient resource")
);
SchemaRequest request = new SchemaRequest("org.apache.camel", "camel-fhir", "4.14.0", objects);

// Generate schemas
SchemaResponse response = generator.generateSchemas(request);
```

## Maven Dependencies

Add the following dependency to your Maven project:

```xml
<dependency>
    <groupId>org.apache.camel.forage</groupId>
    <artifactId>forage-json-schemas</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

## JSON Schema Features

The generated JSON schemas include:

- **Schema Version**: Uses JSON Schema Draft 2020-12
- **Enum Handling**: Flattened enums using `toString()` method
- **Field Inclusion**: Public and non-public fields with getters
- **Jackson Support**: Full Jackson annotation support
- **Type Safety**: Proper type definitions for all supported Java types

## Error Handling

The module provides detailed error messages for common issues:

- **Dependency Not Found**: When Maven coordinates don't resolve to valid artifacts
- **Class Not Found**: When specified Java classes aren't in the dependency
- **Schema Generation Failed**: When class structure prevents schema generation
- **Network Issues**: When Maven repositories are unreachable

## Examples

### Example 1: FHIR Models

Input:
```json
[
  {
    "groupId": "org.apache.camel",
    "artifactId": "camel-fhir",
    "version": "4.14.0",
    "objects": ["org.hl7.fhir.r4.model.Patient"]
  }
]
```

This will generate a comprehensive JSON schema for the FHIR Patient model including all properties, data types, and validation rules.

### Example 2: Multiple Components

Input:
```json
[
  {
    "groupId": "org.apache.camel",
    "artifactId": "camel-http",
    "version": "4.14.0",
    "objects": ["org.apache.camel.component.http.HttpConfiguration"]
  },
  {
    "groupId": "org.apache.camel",
    "artifactId": "camel-jms",
    "version": "4.14.0",
    "objects": ["org.apache.camel.component.jms.JmsConfiguration"]
  }
]
```

This will process multiple Camel components and generate schemas for their configuration classes.

## ServiceLoader Integration

The module is automatically discovered via ServiceLoader mechanism as a `BeanProvider<JsonSchemaGenerator>` with the identifier `json-schema-generator`.

## Thread Safety

All classes in this module are thread-safe and can be used concurrently from multiple threads.