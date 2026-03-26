# Single Database

[:material-github: Source](https://github.com/KaotoIO/forage-examples/tree/main/datasource/single){ .md-button .md-button--primary }

Connect to a single PostgreSQL database and query it using the SQL, JDBC, and Spring-JDBC Camel components.

## What You'll Learn

- How to configure a single datasource with Forage properties
- How Forage auto-registers a `dataSource` bean in the Camel registry
- Querying a database using the `sql`, `jdbc`, and `spring-jdbc` components
- Connection pool and transaction timeout settings

## Prerequisites

Start a PostgreSQL instance:

```bash
camel infra run postgres
```

Then create the sample table and seed data:

```bash
./setup-db.sh
```

??? note "What `setup-db.sh` does"
    ```sql
    CREATE TABLE IF NOT EXISTS bar (
        id INTEGER PRIMARY KEY,
        content VARCHAR(255)
    );

    INSERT INTO bar VALUES (1, 'postgres 1') ON CONFLICT (id) DO NOTHING;
    INSERT INTO bar VALUES (2, 'postgres 2') ON CONFLICT (id) DO NOTHING;
    ```

## Configuration

```properties title="application.properties"
# Database connection
forage.jdbc.db.kind=postgresql                               # (1)!
forage.jdbc.url=jdbc:postgresql://localhost:5432/postgres     # (2)!
forage.jdbc.username=test
forage.jdbc.password=test

# Connection pool
forage.jdbc.pool.initial.size=5                              # (3)!
forage.jdbc.pool.min.size=2
forage.jdbc.pool.max.size=20
forage.jdbc.pool.acquisition.timeout.seconds=5
forage.jdbc.pool.validation.timeout.seconds=3
forage.jdbc.pool.leak.timeout.minutes=10
forage.jdbc.pool.idle.validation.timeout.minutes=3

# Transaction
forage.jdbc.transaction.timeout.seconds=30                   # (4)!
```

1. The driver kind tells Forage which JDBC driver to use.
2. Standard JDBC connection URL.
3. Pool settings control the Agroal connection pool that wraps the raw driver.
4. Default transaction timeout for all operations on this datasource.

Because no named prefix is used (e.g., `forage.myDb.jdbc.*`), Forage registers the datasource under the default name **`dataSource`**. This is the name Camel's `sql` component looks up automatically.

## Route

=== "YAML"

    ```yaml title="route.camel.yaml"
    - route:
        from:
          uri: timer
          parameters:
            period: "1000"
            timerName: template
          steps:
            # sql component picks up "dataSource" by default
            - to:
                uri: sql
                parameters:
                  query: select * from bar
            - log:
                message: "from sql default ds - ${body}"

    - route:
        from:
          uri: timer
          parameters:
            period: "1000"
            timerName: template
          steps:
            - setBody:
                simple:
                  expression: select * from bar
            # jdbc component requires an explicit bean reference
            - to:
                uri: jdbc
                parameters:
                  dataSourceName: dataSource
            - log:
                message: "from jdbc default ds - ${body}"

    - route:
        from:
          uri: timer
          parameters:
            period: "1000"
            timerName: template
          steps:
            - setBody:
                simple:
                  expression: select * from bar
            - to:
                uri: spring-jdbc
                parameters:
                  dataSourceName: dataSource
            - log:
                message: "from spring-jdbc default ds - ${body}"
    ```

=== "Java"

    ```java title="Route.java"
    public class Route extends RouteBuilder {

        @Override
        public void configure() throws Exception {
            from("timer:java?period=1000")
                    .to("sql:select * from bar")
                    .log("from sql default ds - ${body}");

            from("timer:java?period=1000")
                    .setBody(constant("select * from bar"))
                    .to("jdbc:dataSource")
                    .log("from jdbc default ds - ${body}");

            from("timer:java?period=1000")
                    .setBody(constant("select * from bar"))
                    .to("spring-jdbc:dataSource")
                    .log("from spring-jdbc default ds - ${body}");
        }
    }
    ```

Three routes demonstrate the same query through different Camel components:

- **`sql`** -- accepts the query inline and automatically looks up `dataSource`.
- **`jdbc`** -- requires the query in the message body and a named datasource reference.
- **`spring-jdbc`** -- same as `jdbc` but uses Spring's `JdbcTemplate` under the hood.

## Running

```bash
camel forage run route.camel.yaml application.properties
```

You should see repeated log lines with the two rows from the `bar` table.

## Key Takeaways

- A single unnamed Forage JDBC configuration creates a bean called **`dataSource`**.
- The `sql` component discovers this bean automatically; `jdbc` and `spring-jdbc` require an explicit reference.
- Connection pooling and transaction timeouts are configured declaratively in properties.
