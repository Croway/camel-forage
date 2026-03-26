# Camel JBang

The Forage plugin for [Camel JBang](https://camel.apache.org/manual/camel-jbang.html) provides dependency resolution, property validation, hot reload, and export to production runtimes.

## Installation

```bash
camel plugin add forage
```

## Running Routes

```bash
camel forage run *
```

The `*` glob picks up all route files and properties in the current directory. You can also specify files explicitly:

```bash
camel forage run route.camel.yaml application.properties
```

Unlike plain `camel run`, the Forage plugin automatically resolves all required dependencies based on your configuration — no `--dep` flags needed.

## Property Validation

The plugin validates your Forage properties against the catalog before running. This catches configuration errors early.

### Typo Detection

```properties
# Typo in property name
forage.myDb.jdbc.usernam=admin
```

```text
[UNKNOWN_PROPERTY] in application.properties
  Property: forage.myDb.jdbc.usernam
  Unknown property 'usernam' for factory 'jdbc'. Did you mean 'username'?
```

### Invalid Bean Values

```properties
# Invalid database kind
forage.myDb.jdbc.db.kind=postgresqll
```

```text
[INVALID_BEAN_VALUE] in application.properties
  Property: forage.myDb.jdbc.db.kind
  Unknown database 'postgresqll'. Did you mean 'postgresql'?
  Valid options: postgresql, mysql, mariadb, db2, h2, oracle
```

### Unknown Properties

```properties
# Property that doesn't exist
forage.myDb.jdbc.invalid.property=value
```

```text
[UNKNOWN_PROPERTY] in application.properties
  Property: forage.myDb.jdbc.invalid.property
  Unknown property 'invalid.property' for factory 'jdbc'
```

## Hot Reload

In dev mode, Forage watches `.properties` files for changes and automatically recreates beans without restarting:

```bash
camel forage run --dev *
```

When you edit a properties file, Forage:

1. Clears the configuration cache
2. Destroys old beans
3. Creates new beans with updated configuration
4. Resets Camel components so routes pick up the new beans

### What Can Be Hot-Reloaded

- API keys, connection URLs, model names
- Connection pool sizes, timeouts
- Any `forage.*` property value

### What Requires a Restart

- **Provider type changes** — e.g., changing `db.kind` from `postgresql` to `mysql` (requires different JARs)
- **Adding new factory types** — e.g., adding JDBC when only AI was configured
- **Environment variable changes** — the file watcher monitors `.properties` files only

## Exporting to Production

Export your project to Spring Boot or Quarkus with all Forage dependencies included:

=== "Spring Boot"

    ```bash
    camel forage export --runtime=spring-boot --directory=./my-app
    ```

=== "Quarkus"

    ```bash
    camel forage export --runtime=quarkus --directory=./my-app
    ```

## Testing Connections

Verify datasource connectivity without starting routes:

```bash
camel forage datasource test-connection
```

## Configuration Commands

Read Forage configuration and output bean definitions as JSON:

```bash
camel forage config read
```

Write configuration from JSON input:

```bash
camel forage config write --input '{"forage.myDb.jdbc.url":"jdbc:postgresql://localhost:5432/mydb"}'
```
