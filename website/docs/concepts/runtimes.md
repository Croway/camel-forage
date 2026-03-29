# Runtime Support

Forage works with all major Apache Camel runtimes. The same properties and routes work everywhere — only the packaging changes.

## Supported Runtimes

### Camel JBang

The fastest way to develop. Run routes directly from the command line:

```bash
camel run *
```

With the Forage plugin installed, the standard `camel run` command automatically resolves Forage dependencies, validates properties, and starts your routes. No build step needed — ideal for prototyping and development.

### Spring Boot

For production deployments with the Spring ecosystem. Export your project:

```bash
camel export --runtime=spring-boot --directory=./my-app
```

Forage provides Spring Boot starters that integrate with Spring's auto-configuration. Your Forage beans are registered as Spring beans and participate in Spring's dependency injection, health checks, and lifecycle management.

### Quarkus

For cloud-native deployments with fast startup and low memory. Export your project:

```bash
camel export --runtime=quarkus --directory=./my-app
```

Forage provides Quarkus deployment extensions that process configuration at build time. This enables GraalVM native compilation for minimal container images.

## Same Configuration, Every Runtime

The key benefit of Forage is that your configuration is runtime-agnostic. These properties work identically on all three runtimes:

```properties
forage.myDb.jdbc.db.kind=postgresql
forage.myDb.jdbc.url=jdbc:postgresql://localhost:5432/orders
forage.myDb.jdbc.username=admin
forage.myDb.jdbc.password=secret
```

Your Camel routes don't change either:

```yaml
- route:
    from:
      uri: timer:query
      parameters:
        period: "5000"
      steps:
        - to:
            uri: sql
            parameters:
              query: select * from orders
              dataSource: "#myDb"
        - log:
            message: "${body}"
```

## Development Workflow

A typical workflow uses all three stages:

1. **Develop** with Camel JBang — fast iteration, no build step

    ```bash
    camel run *
    ```

2. **Export** to your target runtime when ready

    ```bash
    camel export --runtime=spring-boot --directory=./my-app
    ```

3. **Deploy** using standard tooling for that runtime

    ```bash
    cd my-app
    mvn package
    java -jar target/my-app.jar
    ```

## Runtime Differences

While the configuration is the same, there are a few runtime-specific behaviors to be aware of:

| Feature | Camel JBang | Spring Boot | Quarkus |
|---|---|---|---|
| Startup time | Fast (JVM) | Moderate (Spring context) | Fast (build-time processing) |
| Native compilation | No | No | Yes (GraalVM) |
| Configuration sources | Properties files, env vars | Spring Environment + Forage properties | SmallRye Config + Forage properties |
| Bean registration | Camel registry | Spring beans + Camel registry | CDI beans + Camel registry |
| Hot reload | `camel run --dev` | Spring DevTools | Quarkus dev mode |

In Spring Boot, Forage properties are available through Spring's `@Value` and `@ConditionalOnProperty` annotations. In Quarkus, Forage properties are translated to Quarkus-native configuration at build time.

For most use cases, you don't need to think about these differences — Forage handles the integration transparently.
