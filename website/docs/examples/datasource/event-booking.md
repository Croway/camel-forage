# Event Booking

[:material-github: Source](https://github.com/KaotoIO/forage-examples/tree/main/datasource/event-booking){ .md-button .md-button--primary }

A transactional event booking system that atomically reserves seats and creates booking records, with automatic rollback when an event is sold out.

## What You'll Learn

- How to enable Forage-managed transactions with `forage.jdbc.transaction.enabled=true`
- Using Camel's `transacted` DSL for ACID database operations
- Optimistic concurrency control with conditional SQL updates
- File-based event-driven processing with error handling

## Prerequisites

Start PostgreSQL:

```bash
camel infra run postgres
```

Then create the `events` and `bookings` tables with sample data:

```bash
./setup-db.sh
```

??? note "What `setup-db.sh` does"
    ```sql
    CREATE TABLE IF NOT EXISTS events (
        event_id SERIAL PRIMARY KEY,
        name VARCHAR(255) NOT NULL,
        available_seats INT NOT NULL CHECK (available_seats >= 0)
    );

    CREATE TABLE IF NOT EXISTS bookings (
        booking_id SERIAL PRIMARY KEY,
        event_id INT NOT NULL REFERENCES events(event_id),
        user_id INT NOT NULL,
        booking_time TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
    );

    CREATE INDEX IF NOT EXISTS idx_bookings_event_id ON bookings(event_id);

    -- Sample events
    INSERT INTO events (name, available_seats) VALUES ('Camel Development Conference', 150);
    INSERT INTO events (name, available_seats) VALUES ('Advanced Messaging Workshop', 1);
    ```

    The second event has only **1 seat** -- this is used to demonstrate the sold-out rollback scenario.

## Configuration

```properties title="application.properties"
# Database connection
forage.jdbc.url=jdbc:postgresql://localhost:5432/postgres         # (1)!
forage.jdbc.username=test
forage.jdbc.password=test
forage.jdbc.db.kind=postgresql

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
```

1. Uses the default (unnamed) prefix, so the datasource bean is registered as `dataSource`.
2. Enables Forage-managed transactions. This registers a `PlatformTransactionManager` that Camel's `transacted` DSL can discover automatically.

## Route

```yaml title="book.camel.yaml"
# Route 1: File watcher -- picks up JSON booking files
- route:
    id: file-to-booking-route
    from:
      uri: file
      parameters:
        delete: true
        directoryName: data/inbox
        moveFailed: .error                                       # (1)!
      steps:
        - log:
            message: "New booking file detected: ${header.CamelFileName}"
        - to:
            uri: direct
            parameters:
              name: bookEvent
        - log:
            message: "Successfully processed booking file: ${header.CamelFileNameOnly}"

# Route 2: Transactional booking logic
- route:
    id: event-booking-route
    from:
      uri: direct
      parameters:
        name: bookEvent
      steps:
        - transacted: {}                                         # (2)!
        - log:
            message: >-
              Attempting to book seat for event ${jq(.eventId)}
              for user ${jq(.userId)}
        - toD:                                                   # (3)!
            uri: sql
            parameters:
              dataSource: "#dataSource"
              query: >-
                UPDATE events SET available_seats = available_seats - 1
                WHERE event_id = ${jq(.eventId)} AND available_seats > 0
        - choice:
            when:
              - simple:
                  expression: ${header.CamelSqlUpdateCount} == 1 # (4)!
                steps:
                  - log:
                      message: "Seat successfully reserved for event ${jq(.eventId)}."
                  - toD:
                      uri: sql
                      parameters:
                        dataSource: "#dataSource"
                        query: >-
                          INSERT INTO bookings (event_id, user_id)
                          VALUES (${jq(.eventId)}, ${jq(.userId)})
                  - log:
                      message: "Booking created for user ${jq(.userId)}. Transaction complete."
            otherwise:
              steps:
                - log:
                    message: "Event ${jq(.eventId)} is sold out! Rolling back transaction."
                - throwException:                                # (5)!
                    exceptionType: java.lang.RuntimeException
                    message: Event is sold out
```

1. Failed bookings move the file to `data/inbox/.error` instead of deleting it.
2. Starts a JTA transaction -- all subsequent SQL statements participate in the same transaction.
3. `toD` (dynamic to) is used because the query contains `jq` expressions resolved at runtime.
4. The `WHERE available_seats > 0` guard means the UPDATE affects 0 rows when the event is sold out.
5. Throwing an exception inside a `transacted` block triggers an automatic rollback.

### Transaction flow

1. The `transacted` step begins a database transaction.
2. An `UPDATE` decrements `available_seats` only if seats remain (`available_seats > 0`).
3. If exactly one row was updated, a booking record is inserted and the transaction commits.
4. If zero rows were updated (sold out), an exception is thrown and the entire transaction rolls back -- the seat count stays unchanged.

## Running

```bash
camel forage run book.camel.yaml application.properties
```

### Testing the scenarios

Copy the sample booking files into the inbox to exercise the three scenarios:

```bash
# Successful booking -- "Camel Development Conference" has 150 seats
cp booking-1.json data/inbox/

# Last seat -- "Advanced Messaging Workshop" has 1 seat
cp booking-2.json data/inbox/

# Sold out -- same event, 0 seats remaining after booking-2
cp booking-3.json data/inbox/
```

| File | Event | Outcome |
|---|---|---|
| `booking-1.json` | Camel Development Conference (150 seats) | Booking created, seats: 150 -> 149 |
| `booking-2.json` | Advanced Messaging Workshop (1 seat) | Booking created, seats: 1 -> 0 |
| `booking-3.json` | Advanced Messaging Workshop (0 seats) | Transaction rolled back, file moved to `.error` |

## Key Takeaways

- Setting `forage.jdbc.transaction.enabled=true` registers a transaction manager that Camel's `transacted` DSL discovers automatically.
- Optimistic concurrency (`WHERE available_seats > 0`) prevents overselling without pessimistic locks.
- Throwing an exception inside a `transacted` block rolls back all SQL statements in that transaction.
- The file component's `moveFailed` parameter provides a dead-letter directory for failed bookings.
