# JMS

Forage creates pooled JMS connection factories with optional XA transaction support.

## Quick Start

```properties
forage.myBroker.jms.kind=artemis
forage.myBroker.jms.url=tcp://localhost:61616
forage.myBroker.jms.username=admin
forage.myBroker.jms.password=secret
```

```yaml
- to:
    uri: jms:queue:orders
    parameters:
      connectionFactory: "#myBroker"
```

## Supported Brokers

{{ forage_beans_table("JMS Connection", "jakarta.jms.ConnectionFactory") }}

## Properties

{{ forage_properties("JMS Connection") }}

## Multiple Brokers

```properties
forage.primaryBroker.jms.kind=artemis
forage.primaryBroker.jms.url=tcp://broker1:61616

forage.backupBroker.jms.kind=artemis
forage.backupBroker.jms.url=tcp://broker2:61617
```

## XA Transactions

Setting `forage.jms.transaction.enabled=true` switches the module to XA mode:

- The connection factory becomes an XA-aware pool (`JmsPoolXAConnectionFactory`) that enlists
  sessions in the Narayana transaction manager.
- The Narayana transaction manager is initialized from the `forage.jms.transaction.*` properties.
- JTA transaction policies (`PROPAGATION_REQUIRED`, `REQUIRES_NEW`, ...) are registered in the
  Camel registry for use with the `transacted` EIP.
- The Camel JMS component is configured with a JTA transaction manager, so consumers receive
  each message inside a JTA transaction and a rollback returns the message to the broker.

!!! warning "Endpoint contract"
    Leave `transacted` at its default (`false`) on `jms:` endpoints. The JTA transaction manager
    wired into the component drives the transaction; enabling the endpoint's *local* JMS
    transaction on an XA connection is rejected by brokers such as IBM MQ
    (`MQRC_SYNCPOINT_NOT_AVAILABLE`, reason code 2072). Use `cacheLevelName: CACHE_NONE` on
    transactional consumers.
