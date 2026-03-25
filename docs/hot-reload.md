# Forage Hot-Reload

Forage supports hot-reloading of configuration properties when running in dev mode. When a `.properties` file containing `forage.*` keys is modified, Forage automatically destroys and recreates the affected beans with the updated configuration — without restarting the application.

## Activation

Hot-reload is enabled when **either** condition is true:

- **Camel JBang dev mode:** `camel forage run --dev myroute.yaml`
- **System property:** `-Dforage.reload.enabled=true`

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
- **Environment variable changes** — The file watcher monitors `.properties` files on disk. Changes to environment variables (e.g., `FORAGE_OPENAI_API_KEY`) are not detected. Restart the application or update the corresponding properties file instead.

## How it works

Forage registers a file watcher (`ForageReloadWatcher`) that monitors the working directory for changes to `.properties` files. When a change is detected in a file containing `forage.*` keys, the following reload cycle executes:

1. **Cleanup** — Each `BeanFactory` unbinds old beans from the Camel registry. AutoCloseable resources (e.g., DataSource connection pools, JMS connection factories) are intentionally **not** closed at this stage to avoid breaking in-flight requests — they are left for garbage collection after components are reset.
2. **Clear config** — The `ConfigStore` cache is cleared so values are re-read from disk.
3. **Reconfigure** — Each `BeanFactory` re-reads configuration, creates new bean instances, and binds them to the Camel registry.
4. **Reset components** — Camel components (e.g., `SqlComponent`, `JmsComponent`) that cache bean references are removed so they are recreated with fresh references when routes restart.
5. **Reload routes** — Routes are reloaded so they pick up the new beans and components.

Route files (YAML, XML) are handled separately by Camel's built-in route reload mechanism.
