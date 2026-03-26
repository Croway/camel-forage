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
