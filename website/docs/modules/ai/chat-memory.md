# Chat Memory

Memory providers store conversation history for AI agents. Select one with the `memory.kind` property.

## Available Providers

{{ forage_beans_table("Agent", "Memory") }}

## Message Window

In-memory sliding window that retains the last N messages. Simple and fast — no external infrastructure needed.

```properties
forage.myAgent.agent.features=memory
forage.myAgent.agent.memory.kind=message-window
forage.myAgent.agent.memory.max.messages=20
```

{{ forage_bean_properties("Agent", "Memory", "message-window") }}

## Redis

Persistent conversation storage using Redis. Conversations survive application restarts.

```properties
forage.myAgent.agent.features=memory
forage.myAgent.agent.memory.kind=redis
forage.myAgent.agent.memory.redis.host=localhost
forage.myAgent.agent.memory.redis.port=6379
```

{{ forage_bean_properties("Agent", "Memory", "redis") }}

## Infinispan

Distributed conversation storage using Infinispan. Suitable for clustered deployments.

```properties
forage.myAgent.agent.features=memory
forage.myAgent.agent.memory.kind=infinispan
forage.myAgent.agent.memory.infinispan.host=localhost
forage.myAgent.agent.memory.infinispan.port=11222
```

{{ forage_bean_properties("Agent", "Memory", "infinispan") }}
