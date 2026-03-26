# Embeddings

Embedding models convert text into vector representations for similarity search and RAG.

## Available Providers

{{ forage_beans_table("Agent", "Embeddings Model") }}

## Ollama Embeddings

```properties
forage.myAgent.agent.embeddings.kind=ollama
forage.myAgent.agent.embeddings.model.name=nomic-embed-text
forage.myAgent.agent.embeddings.base.url=http://localhost:11434
```

{{ forage_bean_properties("Agent", "Embeddings Model", "ollama") }}
