# Troubleshooting

This guide helps you diagnose and resolve common issues when using Forage.

## Quick Diagnostics

Before diving into specific issues, try these diagnostic steps:

```bash
# Validate properties without running
camel run * --strict

# Enable debug logging
camel run * --logging-level=DEBUG

# Check Forage plugin installation
camel plugin list | grep forage
```

---

## Common Configuration Issues

### Missing Required Properties

**Error:**
```
MissingConfigException: Missing required configuration: forage.myDb.jdbc.url
```

**Causes:**
- Property not set in any configuration source
- Typo in property name
- Wrong file location

**Solutions:**

1. **Check property naming:**
   ```properties
   # ❌ Wrong
   forage.myDb.jdbc.ur=jdbc:postgresql://localhost:5432/db
   
   # ✅ Correct
   forage.myDb.jdbc.url=jdbc:postgresql://localhost:5432/db
   ```

2. **Verify file location:**
   - Properties files must be in the working directory
   - Or set `FORAGE_CONFIG_DIR` environment variable
   - Or include in classpath

3. **Check environment variable format:**
   ```bash
   # ❌ Wrong
   export forage.myDb.jdbc.url=jdbc:postgresql://localhost:5432/db
   
   # ✅ Correct
   export FORAGE_MYDB_JDBC_URL=jdbc:postgresql://localhost:5432/db
   ```

### Property Typos and Unknown Properties

**Error:**
```
[UNKNOWN_PROPERTY] Unknown property 'usernam' for factory 'jdbc'. Did you mean 'username'?
```

**Solution:**

Use property validation to catch typos before runtime:

```bash
# Validate and show suggestions
camel run * --strict
```

The validator uses Levenshtein distance to suggest corrections. See the [Property Validation](../guides/camel-jbang.md#property-validation) guide for details.

### Bean Reference Errors

**Error:**
```
No bean found with name 'myDatabase'
```

**Causes:**
- Bean name doesn't match property prefix
- Provider not on classpath
- ServiceLoader registration missing

**Solutions:**

1. **Verify bean name matches prefix:**
   ```properties
   # Bean name is "myDatabase"
   forage.myDatabase.jdbc.url=jdbc:postgresql://localhost:5432/db
   ```
   
   ```yaml
   # Reference must match exactly
   - to:
       uri: sql
       parameters:
         dataSource: "#myDatabase"  # Must match prefix
   ```

2. **Check provider dependency:**
   ```xml
   <!-- Required for PostgreSQL -->
   <dependency>
       <groupId>io.kaoto.forage</groupId>
       <artifactId>forage-jdbc-postgresql</artifactId>
       <version>{{ forage_version }}</version>
   </dependency>
   ```

3. **Verify ServiceLoader registration:**
   - Check `META-INF/services/` file exists in JAR
   - Ensure provider class is on classpath

### Invalid Bean Values

**Error:**
```
[INVALID_BEAN_VALUE] Unknown database 'postgresqll'. Did you mean 'postgresql'?
Valid options: postgresql, mysql, mariadb, db2, h2, oracle
```

**Solution:**

Check the catalog for valid bean names:

```bash
# List available providers
camel forage config read --filter jdbc
```

Common typos:
- `postgresqll` → `postgresql`
- `opena` → `openai`
- `olama` → `ollama`

---

## Runtime-Specific Issues

### Camel JBang

#### Plugin Not Found

**Error:**
```
Unknown command: forage
```

**Solution:**

Install or reinstall the plugin:

```bash
camel plugin add forage --gav io.kaoto.forage:camel-jbang-plugin-forage:{{ forage_version }}
```

Verify installation:

```bash
camel plugin list | grep forage
```

#### Dependencies Not Resolved

**Error:**
```
ClassNotFoundException: io.kaoto.forage.jdbc.postgresql.PostgresqlJdbc
```

**Cause:**
- No `forage.*` properties in configuration files
- Plugin not detecting properties

**Solution:**

1. Ensure properties file exists with `forage.*` keys
2. Run from directory containing properties files
3. Check plugin is installed: `camel plugin list`

#### Hot-Reload Not Working

**Cause:**
- Not running in dev mode
- System property not set

**Solution:**

Enable hot-reload:

```bash
# Option 1: Dev mode (recommended)
camel run --dev *

# Option 2: System property
camel run * -Dforage.reload.enabled=true
```

See the hot-reload documentation for details.

### Spring Boot

#### Beans Not Auto-Configured

**Error:**
```
No qualifying bean of type 'javax.sql.DataSource' available
```

**Causes:**
- Missing starter dependency
- `AutoConfiguration.imports` not found
- Properties not in Spring Environment

**Solutions:**

1. **Add starter dependency:**
   ```xml
   <dependency>
       <groupId>io.kaoto.forage</groupId>
       <artifactId>forage-jdbc-starter</artifactId>
       <version>{{ forage_version }}</version>
   </dependency>
   ```

2. **Check auto-configuration:**
   ```bash
   # Enable debug logging
   java -jar app.jar --debug
   ```
   
   Look for `ForageDataSourceAutoConfiguration` in output.

3. **Verify properties location:**
   - Must be in `application.properties` or `application.yml`
   - Or use `@PropertySource` to load custom files

#### ConditionalOnMissingBean Conflicts

**Issue:**
Spring Boot's own auto-configuration runs before Forage beans are registered.

**Solution:**

Forage uses `ImportBeanDefinitionRegistrar` to register beans early, before `@ConditionalOnMissingBean` evaluation. This should work automatically, but if you encounter issues:

```java
// Explicitly exclude Spring Boot's DataSource auto-config
@SpringBootApplication(exclude = DataSourceAutoConfiguration.class)
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

#### Property Precedence Issues

**Issue:**
Spring Environment properties override Forage ConfigStore.

**Expected behavior:**
1. Environment variables (highest)
2. System properties
3. Spring application.properties
4. Forage properties files (lowest)

**Solution:**

Use Spring's property syntax in `application.properties`:

```properties
# Spring Boot style
forage.myDb.jdbc.url=${DATABASE_URL:jdbc:postgresql://localhost:5432/db}
forage.myDb.jdbc.username=${DB_USER:admin}
```

### Quarkus

#### Native Compilation Failures

**Error:**
```
ClassNotFoundException during native image build
```

**Causes:**
- Missing reflection configuration
- Provider not registered for reflection

**Solutions:**

1. **Check reflection configuration:**
   
   Forage providers should be automatically registered, but verify:
   
   ```bash
   # Build with trace
   mvn clean package -Pnative -Dquarkus.native.additional-build-args=--trace-class-initialization
   ```

2. **Manual reflection registration (if needed):**
   
   Create `src/main/resources/META-INF/native-image/reflect-config.json`:
   
   ```json
   [
     {
       "name": "io.kaoto.forage.jdbc.postgresql.PostgresqlJdbc",
       "allDeclaredConstructors": true,
       "allPublicConstructors": true,
       "allDeclaredMethods": true,
       "allPublicMethods": true
     }
   ]
   ```

#### Property Translation Not Working

**Issue:**
Forage properties not translated to Quarkus native properties.

**Cause:**
`ConfigSourceFactory` ordinal incorrect or not registered.

**Solution:**

1. **Verify ConfigSourceFactory registration:**
   
   Check `META-INF/services/io.smallrye.config.ConfigSourceFactory` exists.

2. **Check ordinal:**
   
   Forage uses ordinal 275 (below system properties at 400):
   
   ```java
   @Override
   public int getOrdinal() {
       return 275;
   }
   ```

3. **Test property translation:**
   
   ```bash
   # System properties should override
   mvn quarkus:dev -Dforage.myDb.jdbc.url=jdbc:postgresql://override:5432/db
   ```

#### CDI Injection Issues

**Error:**
```
Unsatisfied dependency for type javax.sql.DataSource
```

**Cause:**
Quarkus expects CDI beans, but Forage registers in Camel registry.

**Solution:**

Forage creates beans in the Camel registry, not as CDI beans. Reference them by name in routes:

```yaml
# ✅ Correct - reference by name
- to:
    uri: sql
    parameters:
      dataSource: "#myDb"

# ❌ Wrong - don't inject
@Inject
DataSource dataSource;  # Won't work
```

---

## Provider-Specific Issues

### JDBC

#### Connection Pool Exhausted

**Error:**
```
Timeout waiting for connection from pool
```

**Solutions:**

1. **Increase pool size:**
   ```properties
   forage.myDb.jdbc.pool.max.size=50  # Default: 20
   ```

2. **Reduce acquisition timeout:**
   ```properties
   forage.myDb.jdbc.pool.acquisition.timeout.seconds=10  # Default: 5
   ```

3. **Check for connection leaks:**
   ```properties
   # Enable leak detection
   forage.myDb.jdbc.pool.leak.timeout.minutes=5  # Default: 10
   ```
   
   Look for warnings in logs:
   ```
   Connection leak detected: connection was not closed
   ```

#### Transaction Timeout

**Error:**
```
Transaction timeout after 30 seconds
```

**Solutions:**

1. **Increase timeout:**
   ```properties
   forage.myDb.jdbc.transaction.timeout.seconds=60  # Default: 30
   ```

2. **Check for long-running queries:**
   
   Enable SQL logging:
   ```properties
   logging.level.org.apache.camel.component.sql=DEBUG
   ```

#### XA Recovery Failures

**Error:**
```
XA transaction recovery failed
```

**Solutions:**

1. **Verify Narayana configuration:**
   ```xml
   <!-- Quarkus -->
   <dependency>
       <groupId>io.quarkus</groupId>
       <artifactId>quarkus-narayana-jta</artifactId>
   </dependency>
   ```

2. **Check transaction logs:**
   ```properties
   # Quarkus
   quarkus.transaction-manager.object-store-directory=target/tx-object-store
   ```

3. **Enable XA debugging:**
   ```properties
   logging.level.com.arjuna=DEBUG
   ```

### JMS

#### Connection Refused

**Error:**
```
Could not connect to broker URL: tcp://localhost:61616
```

**Solutions:**

1. **Verify broker is running:**
   ```bash
   # Check if port is open
   telnet localhost 61616
   ```

2. **Check firewall rules:**
   ```bash
   # Linux
   sudo iptables -L -n | grep 61616
   ```

3. **Verify URL format:**
   ```properties
   # ✅ Correct
   forage.myBroker.jms.broker.url=tcp://localhost:61616
   
   # ❌ Wrong
   forage.myBroker.jms.broker.url=localhost:61616  # Missing protocol
   ```

#### Message Not Consumed

**Issue:**
Messages sent but not received.

**Solutions:**

1. **Check queue/topic name:**
   ```yaml
   # Sender
   - to:
       uri: jms:queue:orders  # Must match
   
   # Receiver
   - from:
       uri: jms:queue:orders  # Must match
   ```

2. **Verify connection factory binding:**
   ```properties
   forage.myBroker.jms.kind=artemis
   forage.myBroker.jms.broker.url=tcp://localhost:61616
   ```
   
   ```yaml
   - from:
       uri: jms:queue:orders
       parameters:
         connectionFactory: "#myBroker"  # Must reference bean
   ```

#### Transaction Rollback

**Error:**
```
Transaction rolled back
```

**Solutions:**

1. **Enable XA transactions:**
   ```properties
   forage.myBroker.jms.transaction.enabled=true
   ```

2. **Check transaction manager:**
   ```bash
   # Verify Narayana is on classpath
   mvn dependency:tree | grep narayana
   ```

3. **Use transacted routes:**
   ```yaml
   - route:
       from:
         uri: jms:queue:orders
         parameters:
           connectionFactory: "#myBroker"
       steps:
         - transacted:
             ref: PROPAGATION_REQUIRED
         - to:
             uri: sql
             parameters:
               dataSource: "#myDb"
   ```

### AI Agents

#### Model Not Found

**Error:**
```
Model 'gpt-5' not found
```

**Solutions:**

1. **Check model name:**
   ```properties
   # ✅ Correct
   forage.myAgent.agent.model.name=gpt-4
   
   # ❌ Wrong
   forage.myAgent.agent.model.name=gpt-5  # Doesn't exist
   ```

2. **Verify provider supports model:**
   
   See [Chat Models](../modules/ai/chat-models.md) for supported models per provider.

#### API Key Invalid

**Error:**
```
401 Unauthorized: Invalid API key
```

**Solutions:**

1. **Check environment variable format:**
   ```bash
   # ✅ Correct
   export FORAGE_MYAGENT_AGENT_API_KEY=sk-...
   
   # ❌ Wrong
   export FORAGE_MYAGENT_API_KEY=sk-...  # Missing 'agent'
   ```

2. **Verify key is valid:**
   ```bash
   # Test OpenAI key
   curl https://api.openai.com/v1/models \
     -H "Authorization: Bearer $FORAGE_MYAGENT_AGENT_API_KEY"
   ```

#### Memory Not Persisting

**Issue:**
Conversation history lost between requests.

**Solutions:**

1. **Set memory ID header:**
   ```yaml
   - setHeader:
       name: CamelLangChain4jAgentMemoryId
       expression:
         simple: "${header.userId}"  # Unique per conversation
   ```

2. **Verify memory provider:**
   ```properties
   forage.myAgent.agent.features=memory
   forage.myAgent.agent.memory.kind=message-window
   ```

3. **Check storage backend:**
   
   For Redis/Infinispan, verify backend is running:
   ```bash
   # Redis
   redis-cli ping
   
   # Infinispan
   curl http://localhost:11222/rest/v2/cache-managers
   ```

#### Tool Not Discovered

**Issue:**
Agent doesn't call tool routes.

**Solutions:**

1. **Verify tool tags match:**
   ```yaml
   # Agent configuration
   - to:
       uri: langchain4j-agent:myAgent
       parameters:
         agent: "#myAgent"
         tags: users  # Must match tool
   
   # Tool route
   - from:
       uri: langchain4j-tools:userDb
       parameters:
         tags: users  # Must match agent
   ```

2. **Check tool description:**
   ```yaml
   - from:
       uri: langchain4j-tools:userDb
       parameters:
         description: "Query user database by ID"  # Clear description
         parameter.userId: string  # Define parameters
   ```

#### Guardrail ClassNotFoundException

**Error:**
```
ClassNotFoundException: com.example.MyGuardrail
```

**Solutions:**

1. **Use fully-qualified class name:**
   ```properties
   # ✅ Correct
   forage.myAgent.guardrails.input.classes=com.example.MyGuardrail
   
   # ❌ Wrong
   forage.myAgent.guardrails.input.classes=MyGuardrail
   ```

2. **Verify class is on classpath:**
   ```bash
   # Check JAR contents
   jar tf target/my-app.jar | grep MyGuardrail
   ```

3. **For Quarkus, register for reflection:**
   ```java
   @RegisterForReflection
   public class MyGuardrail implements InputGuardrail {
       // Implementation
   }
   ```

---

## Debugging Techniques

### Enable Debug Logging

```bash
# Camel JBang
camel run * --logging-level=DEBUG

# Spring Boot
java -jar app.jar --logging.level.io.kaoto.forage=DEBUG

# Quarkus
mvn quarkus:dev -Dquarkus.log.category."io.kaoto.forage".level=DEBUG
```

### Inspect Camel Registry

```java
// List all beans
camelContext.getRegistry().findByType(DataSource.class)
    .forEach((name, bean) -> 
        System.out.println("Bean: " + name + " = " + bean));

// Lookup specific bean
DataSource ds = camelContext.getRegistry()
    .lookupByNameAndType("myDb", DataSource.class);
```

### Validate Properties

```bash
# Strict mode - fail on warnings
camel run * --strict

# Show all warnings
camel run *  # Warnings printed but doesn't fail
```

### Check ServiceLoader

```java
// List discovered providers
ServiceLoader<DataSourceProvider> providers = 
    ServiceLoader.load(DataSourceProvider.class);
providers.forEach(provider -> 
    System.out.println("Provider: " + provider.getClass().getName()));
```

### Test Hot-Reload

```bash
# Start in dev mode
camel run --dev *

# In another terminal, modify properties
echo "forage.myDb.jdbc.pool.max.size=50" >> application.properties

# Watch logs for reload message
# [ForageReloadWatcher] Configuration changed, reloading beans...
```

---

## Error Message Reference

| Error Pattern | Cause | Solution |
|--------------|-------|----------|
| `MissingConfigException: Missing required configuration: forage.*.jdbc.url` | Required property not set | Add property to file or environment |
| `RuntimeForageException: No provider found for kind: postgresqll` | Typo in provider kind | Check spelling, use validation |
| `ServiceConfigurationError: Provider ... could not be instantiated` | ServiceLoader registration issue | Verify `META-INF/services` file |
| `IllegalStateException: No DataSourceProvider found` | Missing dependency | Add provider module to classpath |
| `[UNKNOWN_PROPERTY] Unknown property 'usernam'` | Property typo | Use `--strict` mode for suggestions |
| `[INVALID_BEAN_VALUE] Unknown database 'postgresqll'` | Invalid bean name | Check catalog for valid options |
| `No bean found with name 'myDatabase'` | Bean name mismatch | Verify prefix matches bean reference |
| `Timeout waiting for connection from pool` | Pool exhausted | Increase `pool.max.size` |
| `Transaction timeout after 30 seconds` | Long-running transaction | Increase `transaction.timeout.seconds` |
| `401 Unauthorized: Invalid API key` | Wrong API key | Check environment variable format |
| `ClassNotFoundException: com.example.MyGuardrail` | Guardrail not on classpath | Use fully-qualified name, check JAR |

---

## Getting Help

If you're still stuck after trying these solutions:

1. **Check the documentation:**
   - [Core Concepts](../concepts/index.md)
   - [Configuration System](../concepts/configuration.md)
   - [Module-specific docs](../modules/index.md)

2. **Search existing issues:**
   - [GitHub Issues](https://github.com/KaotoIO/forage/issues)

3. **Ask for help:**
   - [GitHub Discussions](https://github.com/KaotoIO/forage/discussions)
   - Include error messages, configuration, and Forage version

4. **Report a bug:**
   - [Create an issue](https://github.com/KaotoIO/forage/issues/new)
   - Include minimal reproducible example