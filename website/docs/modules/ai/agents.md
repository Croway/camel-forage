# Agents

Forage creates AI agents with configurable chat models, memory providers, and guardrails for LangChain4j integration.

## Quick Start

```properties
forage.myAgent.agent.model.kind=ollama
forage.myAgent.agent.model.name=granite4:3b
forage.myAgent.agent.base.url=http://localhost:11434
forage.myAgent.agent.features=memory
forage.myAgent.agent.memory.kind=message-window
forage.myAgent.agent.memory.max.messages=20
```

```yaml
- to:
    uri: langchain4j-agent:myAgent
    parameters:
      agent: "#myAgent"
```

## Properties

{{ forage_properties("Agent") }}

## Available Chat Models

{{ forage_beans_table("Agent", "Chat Model") }}

## Available Memory Providers

{{ forage_beans_table("Agent", "Memory") }}

## Available Input Guardrails

{{ forage_beans_table("Agent", "Input Guardrail") }}

## Available Output Guardrails

{{ forage_beans_table("Agent", "Output Guardrail") }}
