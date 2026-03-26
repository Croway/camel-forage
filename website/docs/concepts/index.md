# Core Concepts

Forage is built around a few simple ideas. Understanding them will help you get the most out of the library.

## How Forage Works

In a typical Apache Camel application, you write Java code to create beans — datasources, connection factories, AI models — and register them in the Camel registry. Forage replaces that code with configuration:

```
Properties file  →  Forage discovers the right provider  →  Bean registered in Camel registry  →  Route uses it by name
```

You choose a name, configure it with properties, and reference it in your routes. That's it.

## Key Concepts

<div class="grid cards" markdown>

- **[Bean Providers](bean-providers.md)**

    How Forage discovers implementations and creates beans from your configuration.

- **[Configuration](configuration.md)**

    How properties are resolved from files, environment variables, and system properties.

- **[Architecture](architecture.md)**

    How the project is organized into core interfaces, implementations, and runtime adapters.

- **[Runtime Support](runtimes.md)**

    How the same configuration works across Camel JBang, Spring Boot, and Quarkus.

</div>
