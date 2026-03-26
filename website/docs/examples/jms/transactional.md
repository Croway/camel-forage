# Transactional JMS

[:material-github: Source](https://github.com/KaotoIO/forage-examples/tree/main/jms/transactional){ .md-button .md-button--primary }

XA transactions with Narayana transaction manager, demonstrating automatic rollback, redelivery, and dead letter queue handling.

## What You'll Learn

- How Forage configures XA transactions and the Narayana transaction manager from properties
- Using `PROPAGATION_REQUIRED` and other transaction policies in routes
- How failed messages are automatically rolled back and redelivered by the broker
- Dead letter queue (DLQ) handling after maximum redelivery attempts

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
# JMS provider
forage.myBroker.jms.kind=artemis
forage.myBroker.jms.broker.url=tcp://localhost:61616
forage.myBroker.jms.username=artemis
forage.myBroker.jms.password=artemis

# Connection pool
forage.myBroker.jms.pool.enabled=true
forage.myBroker.jms.pool.max.connections=10
forage.myBroker.jms.pool.max.sessions.per.connection=500
forage.myBroker.jms.pool.idle.timeout.millis=30000
forage.myBroker.jms.pool.connection.timeout.millis=30000
forage.myBroker.jms.pool.block.if.full=true

# XA Transaction settings — this is what enables transactional mode
forage.myBroker.jms.transaction.enabled=true
forage.myBroker.jms.transaction.timeout.seconds=30
forage.myBroker.jms.transaction.node.id=node1
forage.myBroker.jms.transaction.enable.recovery=true
forage.myBroker.jms.transaction.object.store.directory=tx-object-store
forage.myBroker.jms.transaction.object.store.type=file-system
```

Setting `transaction.enabled=true` changes Forage's behavior: it creates an `XAConnectionFactory` instead of a regular `ConnectionFactory`, initializes the Narayana transaction manager, and registers JTA transaction policies (`PROPAGATION_REQUIRED`, `PROPAGATION_REQUIRES_NEW`, etc.) in the Camel registry.

## Route

=== "YAML"

    ```yaml
    # Producer — sends a message to input.queue every 5 seconds
    - route:
        id: producer-route
        from:
          uri: timer:producer
          parameters:
            period: "5000"
          steps:
            - setBody:
                simple:
                  expression: Transactional message
            - to:
                uri: jms
                parameters:
                  destinationName: input.queue
                  destinationType: queue
            - log:
                message: Sent message to input queue

    # Transactional consumer — processes within an XA transaction
    - route:
        id: transactional-consumer-route
        from:
          uri: jms
          parameters:
            destinationName: input.queue
            destinationType: queue
            transacted: "true"
            cacheLevelName: CACHE_NONE
          steps:
            - transacted:
                ref: PROPAGATION_REQUIRED
            - log:
                message: "Processing message: ${body}"
            - choice:
                when:
                  - simple: ${random(0,10)} > 7
                    steps:
                      - log:
                          message: Simulating error - message will be rolled back
                      - throwException:
                          exceptionType: java.lang.RuntimeException
                          message: Simulated processing error
                otherwise:
                  steps:
                    - log:
                        message: Processing successful - committing transaction
                    - to:
                        uri: jms
                        parameters:
                          destinationName: output.queue
                          destinationType: queue
                    - log:
                        message: Message forwarded to output queue

    # Consumer for successfully processed messages
    - route:
        id: output-consumer-route
        from:
          uri: jms
          parameters:
            destinationName: output.queue
            destinationType: queue
          steps:
            - log:
                message: "Successfully processed message: ${body}"

    # Dead letter queue consumer
    - route:
        id: dlq-consumer-route
        from:
          uri: jms
          parameters:
            destinationName: DLQ
            destinationType: queue
          steps:
            - log:
                message: "Message sent to DLQ after max redeliveries: ${body}"
    ```

=== "Java"

    ```java
    public class Route extends RouteBuilder {

        @Override
        public void configure() throws Exception {

            // Producer - sends messages to input queue
            from("timer:producer?period=10000")
                    .setBody(constant("Transactional message"))
                    .to("jms:queue:input.queue")
                    .log("Sent message to input queue");

            // Transactional consumer - processes messages within XA transaction
            from("jms:queue:input.queue?transacted=true")
                    .transacted("PROPAGATION_REQUIRED")
                    .log("Processing message: ${body}")
                    .choice()
                        .when(simple("${random(0,10)} > 7"))
                            .log("Simulating error - message will be rolled back")
                            .throwException(new RuntimeException("Simulated processing error"))
                        .otherwise()
                            .log("Processing successful - committing transaction")
                            .to("jms:queue:output.queue")
                            .log("Message forwarded to output queue")
                    .end();

            // Consumer for successfully processed messages
            from("jms:queue:output.queue")
                    .log("Successfully processed message: ${body}");

            // Dead Letter Queue consumer
            from("jms:queue:DLQ")
                    .log("Message sent to DLQ after max redeliveries: ${body}");
        }
    }
    ```

The example has four routes:

1. **Producer** -- a timer sends messages to `input.queue` at a fixed interval.
2. **Transactional consumer** -- consumes from `input.queue` within an XA transaction. Roughly 30% of messages trigger a simulated error, causing the transaction to roll back. Successful messages are forwarded to `output.queue`.
3. **Output consumer** -- logs messages that completed the transaction successfully.
4. **DLQ consumer** -- logs messages that exhausted the broker's maximum redelivery attempts.

Key points in the transactional consumer:

- `transacted: "true"` on the JMS endpoint tells Camel to use transacted message acknowledgment.
- `cacheLevelName: CACHE_NONE` is required for XA transactions to prevent stale session caching.
- `transacted` with `ref: PROPAGATION_REQUIRED` joins an existing transaction or creates a new one.

## Running

```bash
camel forage run *
```

Watch the logs -- you will see successful commits, simulated rollbacks, and eventually messages arriving in the DLQ after repeated failures.

You can also monitor queue depths in the Artemis web console at `http://localhost:8161/console`.

## Key Takeaways

- **One property enables XA** -- setting `transaction.enabled=true` switches from plain `ConnectionFactory` to `XAConnectionFactory` and wires up Narayana automatically.
- **Transaction policies are auto-registered** -- `PROPAGATION_REQUIRED`, `PROPAGATION_REQUIRES_NEW`, and others are available in the registry without any Java configuration.
- **Rollback is automatic** -- any exception within a transacted route causes a full rollback; the message returns to the queue for redelivery.
- **DLQ safety net** -- after the broker's maximum redelivery attempts, messages move to the dead letter queue rather than being lost.
- **Recovery support** -- the file-system object store (`tx-object-store/`) persists transaction logs so Narayana can recover in-doubt transactions after a crash.
