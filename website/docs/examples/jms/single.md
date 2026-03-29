# Basic Messaging

[:material-github: Source](https://github.com/KaotoIO/forage-examples/tree/main/jms/single){ .md-button .md-button--primary }

Basic JMS producer/consumer with auto-configured ActiveMQ Artemis connection pooling.

## What You'll Learn

- How Forage auto-configures a pooled JMS `ConnectionFactory` from properties
- Sending and receiving messages with the Camel JMS component
- Using named bean prefixes to register the connection factory in the Camel registry
- Connection pool tuning for production workloads

## Prerequisites

- Java 17 or later
- [Camel JBang](https://camel.apache.org/manual/camel-jbang.html) with the Forage plugin installed

Start ActiveMQ Artemis:

```bash
camel infra run artemis
```

This starts Artemis on `tcp://localhost:61616` with the web console at `http://localhost:8161/console` (credentials: `artemis` / `artemis`).

## Configuration

Create `application.properties`:

```properties
# JMS provider — "artemis" selects the ActiveMQ Artemis connection factory
forage.myBroker.jms.kind=artemis

# Broker connection URL
forage.myBroker.jms.broker.url=tcp://localhost:61616

# Broker credentials
forage.myBroker.jms.username=artemis
forage.myBroker.jms.password=artemis

# Connection pool settings (uses pooled-jms under the hood)
forage.myBroker.jms.pool.enabled=true
forage.myBroker.jms.pool.max.connections=10
forage.myBroker.jms.pool.max.sessions.per.connection=500
forage.myBroker.jms.pool.idle.timeout.millis=30000
forage.myBroker.jms.pool.connection.timeout.millis=30000
forage.myBroker.jms.pool.block.if.full=true
forage.myBroker.jms.pool.block.if.full.timeout.millis=-1
```

The `myBroker` prefix becomes the bean name. Forage registers a pooled `ConnectionFactory` as `#myBroker` in the Camel registry -- no Java code, no bean definitions.

## Route

=== "YAML"

    ```yaml
    - route:
        id: jms-producer-route
        from:
          uri: timer:producer
          parameters:
            period: "5000"
          steps:
            - setBody:
                simple:
                  expression: Hello from Camel Forage JMS!
            - to:
                uri: jms
                parameters:
                  destinationType: queue
                  destinationName: test.queue
            - log:
                message: Message sent to JMS queue

    - route:
        id: jms-consumer-route
        from:
          uri: jms
          parameters:
            destinationType: queue
            destinationName: test.queue
          steps:
            - log:
                message: "Message received from JMS queue: ${body}"
    ```

=== "Java"

    ```java
    public class Route extends RouteBuilder {

        @Override
        public void configure() throws Exception {
            from("timer:producer?period=5000")
                    .setBody(constant("Hello from Camel Forage JMS!"))
                    .to("jms:queue:test.queue")
                    .log("Message sent to JMS queue");

            from("jms:queue:test.queue")
                    .log("Message received from JMS queue: ${body}");
        }
    }
    ```

The first route fires every 5 seconds, sets a message body, and sends it to `test.queue`. The second route consumes from the same queue and logs each message. Forage automatically wires the `#myBroker` connection factory into the JMS component.

## Running

```bash
camel run *
```

You should see alternating log lines every 5 seconds:

```
Message sent to JMS queue
Message received from JMS queue: Hello from Camel Forage JMS!
```

## Key Takeaways

- **Zero boilerplate** -- a few properties replace all manual `ConnectionFactory` setup, including connection pooling with pooled-jms.
- **Named beans** -- the prefix (`myBroker`) becomes the bean name in the Camel registry, making it easy to reference explicitly or configure multiple brokers.
- **Pool tuning** -- connection pool size, timeouts, and blocking behavior are all configurable through properties.
- **Provider abstraction** -- changing `jms.kind` from `artemis` to `ibmmq` (with the corresponding dependency) switches the underlying provider without touching routes.
