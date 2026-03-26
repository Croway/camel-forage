# Idempotent Consumer

[:material-github: Source](https://github.com/KaotoIO/forage-examples/tree/main/datasource/idempotent){ .md-button .md-button--primary }

Prevent duplicate file processing using a JDBC-backed idempotent consumer repository.

## What You'll Learn

- How Forage creates a JDBC idempotent repository from properties
- Using Camel's `idempotentConsumer` EIP to skip already-processed messages
- Tracking processed file names in PostgreSQL for durability

## Prerequisites

Start PostgreSQL:

```bash
camel infra run postgres
```

No setup script is needed -- Forage automatically creates the idempotent repository table on first use.

## Configuration

```properties title="application.properties"
# Database connection
forage.jdbc.db.kind=postgresql                                    # (1)!
forage.jdbc.url=jdbc:postgresql://localhost:5432/postgres
forage.jdbc.username=test
forage.jdbc.password=test

# Connection pool
forage.jdbc.pool.initial.size=5
forage.jdbc.pool.min.size=2
forage.jdbc.pool.max.size=20
forage.jdbc.pool.acquisition.timeout.seconds=5
forage.jdbc.pool.validation.timeout.seconds=3
forage.jdbc.pool.leak.timeout.minutes=10
forage.jdbc.pool.idle.validation.timeout.minutes=3

# Transaction
forage.jdbc.transaction.timeout.seconds=30
forage.jdbc.transaction.enabled=true                              # (2)!

# Idempotent repository
forage.jdbc.idempotent.repository.enabled=true                    # (3)!
forage.jdbc.idempotent.repository.table.name=camel_idempotent     # (4)!
```

1. Uses the default (unnamed) prefix.
2. Transactions are required for the idempotent repository to guarantee exactly-once semantics.
3. Enables the JDBC idempotent repository.
4. The table name where processed message keys are stored. Forage registers a bean named `camel_idempotent` that the route references.

## Route

```yaml title="jdbc-idempotent.camel.yaml"
- route:
    from:
      uri: file
      parameters:
        delay: 500
        delete: true                                              # (1)!
        directoryName: data/inbox
        idempotent: false                                         # (2)!
        idempotentEager: false
        noop: false
        sortBy: file:modified
        initialDelay: 500
      steps:
        - log:
            message: "Discovered file: ${header.CamelFileName} (size: ${header.CamelFileLength} bytes)"
        - idempotentConsumer:                                     # (3)!
            header:
              expression: CamelFileName                           # (4)!
            idempotentRepository: "#camel_idempotent"             # (5)!
        - log:
            message: "Processed file: ${header.CamelFileName} with content: ${body}"
```

1. Files are deleted after the consumer picks them up.
2. The file component's built-in idempotent filter is disabled -- we use the EIP-level `idempotentConsumer` instead for JDBC-backed persistence.
3. The `idempotentConsumer` step filters out messages whose key has already been recorded.
4. Uses the file name as the unique key.
5. References the JDBC idempotent repository bean created by Forage.

### How it works

1. The file consumer picks up every file from `data/inbox` and logs its discovery.
2. The `idempotentConsumer` checks whether `CamelFileName` already exists in the `camel_idempotent` table.
3. If the key is **new**, the message passes through and the key is recorded -- the "Processed file" log appears.
4. If the key **already exists**, the message is silently filtered -- only the "Discovered file" log appears.

## Running

```bash
camel forage run jdbc-idempotent.camel.yaml application.properties
```

### Testing idempotency

```bash
# First copy -- file is processed
cp test.txt data/inbox/

# Second copy (same name) -- file is discovered but NOT processed
cp test.txt data/inbox/

# Different name -- file is processed
cp test.txt data/inbox/test2.txt
```

Expected output:

```
Discovered file: test.txt (size: 13 bytes)
Processed file: test.txt with content: Test content.

Discovered file: test.txt (size: 13 bytes)
# (no "Processed file" log -- idempotent filter blocked it)

Discovered file: test2.txt (size: 13 bytes)
Processed file: test2.txt with content: Test content.
```

## Key Takeaways

- Setting `forage.jdbc.idempotent.repository.enabled=true` and a `table.name` creates a ready-to-use `JdbcMessageIdRepository` bean.
- The repository persists processed keys in PostgreSQL, so duplicates are rejected even after application restarts.
- The file component's built-in idempotent filter (`idempotent: true`) only works in-memory; using the EIP-level `idempotentConsumer` with a JDBC repository provides durable, cross-restart protection.
- The `CamelFileName` header is a natural choice for file deduplication, but any header or expression can be used as the idempotent key.
