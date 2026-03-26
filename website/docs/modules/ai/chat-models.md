# Chat Models

Forage supports multiple LLM providers. Select one with the `model.kind` property.

## Available Providers

{{ forage_beans_table("Agent", "Chat Model") }}

## Ollama

Locally-hosted models via [Ollama](https://ollama.ai/).

```properties
forage.myAgent.agent.model.kind=ollama
forage.myAgent.agent.model.name=granite4:3b
forage.myAgent.agent.base.url=http://localhost:11434
```

{{ forage_bean_properties("Agent", "Chat Model", "ollama") }}

## OpenAI

OpenAI API and compatible endpoints.

```properties
forage.myAgent.agent.model.kind=openai
forage.myAgent.agent.model.name=gpt-4o
forage.myAgent.agent.api.key=${OPENAI_API_KEY}
```

{{ forage_bean_properties("Agent", "Chat Model", "openai") }}

## Google Gemini

Google's Gemini models.

```properties
forage.myAgent.agent.model.kind=google-gemini
forage.myAgent.agent.model.name=gemini-2.5-flash-lite
forage.myAgent.agent.api.key=${GOOGLE_API_KEY}
```

{{ forage_bean_properties("Agent", "Chat Model", "google-gemini") }}

## Anthropic

Anthropic's Claude models.

```properties
forage.myAgent.agent.model.kind=anthropic
forage.myAgent.agent.model.name=claude-sonnet-4-20250514
forage.myAgent.agent.api.key=${ANTHROPIC_API_KEY}
```

{{ forage_bean_properties("Agent", "Chat Model", "anthropic") }}

## Azure OpenAI

Azure-hosted OpenAI models.

```properties
forage.myAgent.agent.model.kind=azure-openai
forage.myAgent.agent.endpoint=https://my-resource.openai.azure.com
forage.myAgent.agent.deployment.name=my-deployment
forage.myAgent.agent.api.key=${AZURE_OPENAI_KEY}
```

{{ forage_bean_properties("Agent", "Chat Model", "azure-openai") }}

## Amazon Bedrock

AWS Bedrock models.

```properties
forage.myAgent.agent.model.kind=bedrock
forage.myAgent.agent.model.name=anthropic.claude-v2
```

{{ forage_bean_properties("Agent", "Chat Model", "bedrock") }}

## WatsonX AI

IBM WatsonX AI models.

```properties
forage.myAgent.agent.model.kind=watsonx-ai
forage.myAgent.agent.model.name=ibm/granite-34b-code-instruct
forage.myAgent.agent.api.key=${WATSONX_API_KEY}
```

{{ forage_bean_properties("Agent", "Chat Model", "watsonx-ai") }}

## Mistral AI

```properties
forage.myAgent.agent.model.kind=mistral-ai
forage.myAgent.agent.model.name=mistral-large-latest
forage.myAgent.agent.api.key=${MISTRAL_API_KEY}
```

{{ forage_bean_properties("Agent", "Chat Model", "mistral-ai") }}

## Hugging Face

```properties
forage.myAgent.agent.model.kind=hugging-face
forage.myAgent.agent.model.name=meta-llama/Llama-2-7b-chat-hf
forage.myAgent.agent.api.key=${HF_API_KEY}
```

{{ forage_bean_properties("Agent", "Chat Model", "hugging-face") }}

## DashScope

Alibaba Cloud's DashScope models.

```properties
forage.myAgent.agent.model.kind=dashscope
forage.myAgent.agent.model.name=qwen-turbo
forage.myAgent.agent.api.key=${DASHSCOPE_API_KEY}
```

{{ forage_bean_properties("Agent", "Chat Model", "dashscope") }}

## LocalAI

Self-hosted models via [LocalAI](https://localai.io/).

```properties
forage.myAgent.agent.model.kind=local-ai
forage.myAgent.agent.model.name=gpt-4
forage.myAgent.agent.base.url=http://localhost:8080
```

{{ forage_bean_properties("Agent", "Chat Model", "local-ai") }}
