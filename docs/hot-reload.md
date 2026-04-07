# Forage Hot-Reload

Forage supports hot-reloading of configuration properties when running in dev mode. When a `.properties` file containing `forage.*` keys is modified, Forage automatically destroys and recreates the affected beans with the updated configuration — without restarting the application.

## Activation

Hot-reload is enabled when running in Camel JBang dev mode:

- **Camel JBang dev mode:** `camel run --dev myroute.yaml`

Forage hooks into Camel's route watcher reload strategy via the `ContextServicePlugin.onReload()` SPI, so no additional flags or system properties are needed.

## What can be hot-reloaded

Configuration **values** within the same provider type can be changed at runtime:

- API keys (e.g., `forage.openai.api.key`)
- Connection URLs (e.g., `forage.jdbc.url`)
- Model names (e.g., `forage.agent.model.name`)
- Connection parameters (pool sizes, timeouts, etc.)
- Any other `forage.*` property value

## What requires a restart

The following changes **cannot** be hot-reloaded and require restarting the application:

- **Provider type changes** — Changing `forage.jdbc.db.kind` from `postgresql` to `mysql`, or `forage.agent.model.kind` from `openai` to `ollama`. These require different JARs on the classpath, which cannot be added at runtime.
- **Adding new factory types** — Adding JDBC configuration when only AI was initially configured. The required dependencies are resolved at startup.
- **Environment variable changes** — Only `.properties` file changes are detected. Changes to environment variables (e.g., `FORAGE_OPENAI_API_KEY`) are not detected. Restart the application or update the corresponding properties file instead.

## How it works

Forage implements the `ContextServicePlugin.onReload()` SPI, which is called by Camel's route watcher reload strategy **before** routes are reloaded. This ensures beans are refreshed synchronously in a single thread, eliminating race conditions.

When Camel detects a file change in dev mode, the following reload cycle executes:

1. **Cleanup** — Each `BeanFactory` unbinds old beans from the Camel registry.
2. **Clear config** — The `ConfigStore` cache is cleared so values are re-read from disk.
3. **Reconfigure** — Each `BeanFactory` re-reads configuration, creates new bean instances, and binds them to the Camel registry.

After the plugin reload completes, Camel proceeds to reload routes, which pick up the new beans and components automatically.

Route files (YAML, XML) are handled separately by Camel's built-in route reload mechanism.
