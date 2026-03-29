# Aggregation Repository

[:material-github: Source](https://github.com/KaotoIO/forage-examples/tree/main/datasource/aggregation){ .md-button .md-button--primary }

Batch IoT events using Camel's aggregation pattern with a JDBC-backed repository for durability across restarts.

## What You'll Learn

- How Forage creates a JDBC aggregation repository from a single property
- Configuring Camel's aggregator with size-based and timeout-based completion
- Writing a custom `AggregationStrategy` to collect events into a list
- Persisting aggregation state in PostgreSQL

## Prerequisites

Start PostgreSQL:

```bash
camel infra run postgres
```

Then create the aggregation repository tables:

```bash
./setup-db.sh
```

??? note "What `setup-db.sh` does"
    ```sql
    CREATE TABLE IF NOT EXISTS event_aggregation (
        id varchar(255) NOT NULL,
        exchange bytea NOT NULL,
        version BIGINT NOT NULL,
        CONSTRAINT event_aggregation_pk PRIMARY KEY (id)
    );

    CREATE TABLE IF NOT EXISTS event_aggregation_completed (
        id varchar(255) NOT NULL,
        exchange bytea NOT NULL,
        version BIGINT NOT NULL,
        CONSTRAINT event_aggregation_completed_pk PRIMARY KEY (id)
    );
    ```

    Camel's JDBC aggregation repository requires two tables: one for in-progress aggregations and one for completed aggregations. The table names must match the `forage.jdbc.aggregation.repository.name` property.

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
forage.jdbc.transaction.enabled=true                              # (2)!
forage.jdbc.transaction.timeout.seconds=30

# Aggregation repository
forage.jdbc.aggregation.repository.name=event_aggregation         # (3)!
```

1. Uses the default (unnamed) prefix.
2. Transactions are required for the aggregation repository to guarantee consistency.
3. This creates a `JdbcAggregationRepository` bean named `event_aggregation` that stores aggregation state in the `event_aggregation` / `event_aggregation_completed` tables.

## Route

```yaml title="event-batching.camel.yaml"
- route:
    id: event-batching
    from:
      uri: direct
      parameters:
        name: events
      steps:
        - log:
            message: "Received event with id :${header.eventId} and body: ${body}"
        - aggregate:
            aggregationRepository: "#event_aggregation"           # (1)!
            aggregationStrategy: "#groupedBodyAggregationStrategy" # (2)!
            completionSize: 5                                     # (3)!
            completionTimeout: "5000"                             # (4)!
            correlationExpression:
              header:
                expression: eventId                               # (5)!
            steps:
              - log:
                  message: >-
                    Batch complete with ${exchangeProperty.CamelAggregatedSize}
                    event id: ${header.eventId} and events: ${body}

- beans:
    - name: groupedBodyAggregationStrategy
      type: org.forage.MyAggregationStrategy
```

1. References the JDBC aggregation repository bean created by Forage.
2. Custom strategy that collects message bodies into a `List`.
3. Complete the batch when 5 events with the same correlation key arrive.
4. Or complete after 5 seconds of inactivity, whichever comes first.
5. Events are grouped by their `eventId` header -- each unique value gets its own batch.

### Aggregation strategy

The custom `MyAggregationStrategy` collects each incoming exchange body into an `ArrayList`:

```java title="org/forage/MyAggregationStrategy.java"
public class MyAggregationStrategy implements AggregationStrategy {

    @Override
    public Exchange aggregate(Exchange oldExchange, Exchange newExchange) {
        Object newBody = newExchange.getIn().getBody();

        if (oldExchange == null) {
            List<Object> list = new ArrayList<>();
            list.add(newBody);
            newExchange.getIn().setBody(list);
            return newExchange;
        }

        List<Object> list = oldExchange.getIn().getBody(List.class);
        list.add(newBody);
        oldExchange.getIn().setBody(list);
        return oldExchange;
    }
}
```

## Running

```bash
camel run event-batching.camel.yaml application.properties \
  org/forage/MyAggregationStrategy.java
```

### Sending test events

Use `camel cmd send` to push events into the `direct:events` endpoint:

```bash
# 3 events with eventId=1 -- batch completes after 5s timeout
camel cmd send --body="Event 1" --header="eventId=1" \
  --endpoint="direct:events" event-batching
camel cmd send --body="Event 2" --header="eventId=1" \
  --endpoint="direct:events" event-batching
camel cmd send --body="Event 3" --header="eventId=1" \
  --endpoint="direct:events" event-batching

# 5 events with eventId=2 -- batch completes immediately by size
for i in 1 2 3 4 5; do
  camel cmd send --body="Batch $i" --header="eventId=2" \
    --endpoint="direct:events" event-batching
done
```

Expected output:

```
Received event with id :1 and body: Event 1
Received event with id :1 and body: Event 2
Received event with id :1 and body: Event 3
...
Batch complete with 3 event id: 1 and events: [Event 1, Event 2, Event 3]
Batch complete with 5 event id: 2 and events: [Batch 1, Batch 2, Batch 3, Batch 4, Batch 5]
```

- **eventId=1**: Only 3 events arrived, so the batch completed after the 5-second timeout.
- **eventId=2**: All 5 events arrived, so the batch completed immediately by size.

## Key Takeaways

- Setting `forage.jdbc.aggregation.repository.name` creates a ready-to-use `JdbcAggregationRepository` bean.
- The repository persists in-flight aggregation state in PostgreSQL, surviving application restarts.
- Completion criteria (size and timeout) work together -- whichever triggers first completes the batch.
- Events are correlated by a header value, allowing independent batches per event type.
