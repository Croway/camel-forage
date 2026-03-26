# Multiple Databases

[:material-github: Source](https://github.com/KaotoIO/forage-examples/tree/main/datasource/multi){ .md-button .md-button--primary }

Connect to PostgreSQL and MySQL simultaneously using named datasource prefixes.

## What You'll Learn

- How to configure multiple datasources with named prefixes (`forage.<name>.jdbc.*`)
- How named prefixes become bean names in the Camel registry
- Routing queries to different databases in a single integration

## Prerequisites

Start both database engines:

```bash
camel infra run postgres
```

```bash
docker run -e MYSQL_ROOT_PASSWORD=pwd -p3306:3306 mysql:latest
```

Then create the schemas and seed data in both databases:

```bash
./setup-db.sh
```

??? note "What `setup-db.sh` does"
    **PostgreSQL** -- creates a `bar` table with two rows.

    ```sql
    CREATE TABLE IF NOT EXISTS bar (
        id INTEGER PRIMARY KEY,
        content VARCHAR
    );
    INSERT INTO bar VALUES (1, 'postgres 1') ON CONFLICT (id) DO NOTHING;
    INSERT INTO bar VALUES (2, 'postgres 2') ON CONFLICT (id) DO NOTHING;
    ```

    **MySQL** -- creates a `test.foo` table with two rows.

    ```sql
    CREATE DATABASE IF NOT EXISTS test;
    CREATE TABLE IF NOT EXISTS test.foo (
        id INTEGER PRIMARY KEY,
        content VARCHAR(255)
    );
    INSERT IGNORE INTO test.foo VALUES (1, 'mysql 1');
    INSERT IGNORE INTO test.foo VALUES (2, 'mysql 2');
    ```

## Configuration

```properties title="application.properties"
# --- PostgreSQL datasource (registered as bean "ds1") ---
forage.ds1.jdbc.db.kind=postgresql                               # (1)!
forage.ds1.jdbc.url=jdbc:postgresql://localhost:5432/postgres
forage.ds1.jdbc.username=test
forage.ds1.jdbc.password=test

forage.ds1.jdbc.pool.initial.size=5
forage.ds1.jdbc.pool.min.size=2
forage.ds1.jdbc.pool.max.size=20
forage.ds1.jdbc.pool.acquisition.timeout.seconds=5
forage.ds1.jdbc.pool.validation.timeout.seconds=3
forage.ds1.jdbc.pool.leak.timeout.minutes=10
forage.ds1.jdbc.pool.idle.validation.timeout.minutes=3

forage.ds1.jdbc.transaction.timeout.seconds=30

# --- MySQL datasource (registered as bean "ds2") ---
forage.ds2.jdbc.db.kind=mysql                                    # (2)!
forage.ds2.jdbc.url=jdbc:mysql://localhost:3306/test
forage.ds2.jdbc.username=root
forage.ds2.jdbc.password=pwd

forage.ds2.jdbc.pool.initial.size=5
forage.ds2.jdbc.pool.min.size=2
forage.ds2.jdbc.pool.max.size=20
forage.ds2.jdbc.pool.acquisition.timeout.seconds=5
forage.ds2.jdbc.pool.validation.timeout.seconds=3
forage.ds2.jdbc.pool.leak.timeout.minutes=10
forage.ds2.jdbc.pool.idle.validation.timeout.minutes=3

forage.ds2.jdbc.transaction.timeout.seconds=30
```

1. The prefix `ds1` becomes the bean name. Use `#ds1` to reference it in routes.
2. The prefix `ds2` registers a second, independent datasource as bean `ds2`.

!!! tip "Named prefix convention"
    The segment between `forage.` and `.jdbc.*` is the **bean name**. You can choose any name -- `forage.orders.jdbc.*`, `forage.inventory.jdbc.*`, etc. Each prefix creates a separate datasource with its own connection pool.

## Route

=== "YAML"

    ```yaml title="route.camel.yaml"
    # sql component -- reference datasource with "#ds1" / "#ds2"
    - route:
        from:
          uri: timer
          parameters:
            period: "1000"
            timerName: template
          steps:
            - to:
                uri: sql
                parameters:
                  dataSource: "#ds1"
                  query: select * from bar
            - log:
                message: "from sql postgresql ds - ${body}"
            - to:
                uri: sql
                parameters:
                  dataSource: "#ds2"
                  query: select * from test.foo
            - log:
                message: "from sql mysql - ${body}"

    # jdbc component -- bean name goes in the URI
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
                uri: jdbc
                parameters:
                  dataSourceName: ds1
            - log:
                message: "from jdbc postgresql - ${body}"
            - setBody:
                simple:
                  expression: select * from test.foo
            - to:
                uri: jdbc
                parameters:
                  dataSourceName: ds2
            - log:
                message: "from jdbc mysql - ${body}"

    # spring-jdbc component
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
                  dataSourceName: ds1
            - log:
                message: "from jdbc postgresql - ${body}"
            - setBody:
                simple:
                  expression: select * from test.foo
            - to:
                uri: spring-jdbc
                parameters:
                  dataSourceName: ds2
            - log:
                message: "from jdbc mysql - ${body}"
    ```

=== "Java"

    ```java title="Route.java"
    public class Route extends RouteBuilder {

        @Override
        public void configure() throws Exception {
            from("timer:java?period=1000")
                    .to("sql:select * from bar?dataSource=#ds1")
                    .log("from sql postgresql - ${body}")
                    .to("sql:select * from test.foo?dataSource=#ds2")
                    .log("from sql mysql - ${body}");

            from("timer:java?period=1000")
                    .setBody(constant("select * from bar"))
                    .to("jdbc:ds1")
                    .log("from jdbc postgresql - ${body}")
                    .setBody(constant("select * from test.foo"))
                    .to("jdbc:ds2")
                    .log("from jdbc mysql - ${body}");

            from("timer:java?period=1000")
                    .setBody(constant("select * from bar"))
                    .to("spring-jdbc:ds1")
                    .log("from spring jdbc postgresql - ${body}")
                    .setBody(constant("select * from test.foo"))
                    .to("spring-jdbc:ds2")
                    .log("from spring jdbc mysql - ${body}");
        }
    }
    ```

Each route queries both databases in a single flow. The only difference from the [single database](single.md) example is the explicit `dataSource` parameter pointing to the named bean.

## Running

```bash
camel forage run route.camel.yaml application.properties
```

The logs alternate between PostgreSQL and MySQL results on each timer tick.

## Key Takeaways

- Named prefixes (`forage.ds1.jdbc.*`, `forage.ds2.jdbc.*`) create separate beans with independent pools.
- The prefix segment (`ds1`, `ds2`) is the **bean name** used in route references.
- Each datasource can use a different database engine, credentials, and pool settings.
- The `sql` component uses `dataSource: "#beanName"`, while `jdbc` and `spring-jdbc` use `dataSourceName: beanName`.
