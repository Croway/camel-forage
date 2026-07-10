# Configuration Precedence

This document is the single source of truth for how Forage resolves configuration
values. All runtimes (plain Camel, Spring Boot, Quarkus) follow the same contract.

## The contract

When a Forage config class reads a property (e.g. `forage.jdbc.url`), sources are
consulted in this order — the first value found wins:

1. **Environment variables** — the property name converted to `UPPER_SNAKE_CASE`
   (e.g. `FORAGE_JDBC_URL`)
2. **System properties** — the dot-notation property name
   (e.g. `-Dforage.jdbc.url=...`)
3. **Configuration files / runtime configuration** — resolved through the
   `ConfigResolver` chain (see below)

## Where the contract is implemented

Steps 1 and 2 are implemented once, in `ConfigStore.tryRead()`, **before** any
resolver is consulted. Resolvers only supply the third tier, so registering a
runtime-specific resolver can never change the position of environment variables
or system properties in the hierarchy.

## The resolver chain (tier 3)

Resolvers are consulted in descending priority order; the first non-empty value wins:

| Resolver | Priority | Runtime | Sources |
|----------|----------|---------|---------|
| `SpringConfigResolver` | 10 | Spring Boot | Spring `Environment`: profiles, YAML, placeholders, config imports, Cloud Config |
| `DefaultConfigResolver` | 0 | all | runtime detection → Spring Boot / Quarkus / Camel Main `application.properties`, module `forage-*.properties` files |

Registering a resolver replaces any previously registered resolver of the same class
(`ConfigStore.registerResolver`), so a refreshed application context cannot leave a
stale resolver shadowing the new one. Resolvers can be removed with
`ConfigStore.unregisterResolver(Class)`.

## Quarkus specifics

Forage modules translate `forage.*` properties into Quarkus-native properties
(e.g. `quarkus.datasource.*`) through a SmallRye `ConfigSourceFactory`. The translated
config source uses **ordinal 240**, which places it in the Quarkus source hierarchy as:

| Source | Ordinal |
|--------|---------|
| System properties | 400 |
| Environment variables | 300 |
| `.env` file | 295 |
| `application.properties` from `/config` | 260 |
| `application.properties` (application) | 250 |
| **Forage translated properties** | **240** |
| `microprofile-config.properties` | 100 |

An explicit Quarkus-native property set by the user (in `application.properties`,
environment, or `-D`) therefore always overrides the value Forage derives from
`forage.*` keys.

Prefix discovery on Quarkus consults the SmallRye `ConfigSourceContext` in addition
to Forage's own sources, so profile-scoped keys (`%prod.forage.*`) and keys from YAML
config sources are discovered too.

## File loading order (within tier 3)

For a module named `forage-model-openai`, the properties file
`forage-model-openai.properties` is searched in this order:

1. Current working directory
2. Directory from `forage.config.dir` system property or `FORAGE_CONFIG_DIR`
   environment variable
3. Classpath (package-relative, then root), plus pluggable
   `PluggablePropertyFileSource` implementations discovered via `ServiceLoader`
