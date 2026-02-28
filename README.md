# Mini Agent

A self-developed Java Agent Framework based on Spring Boot and AI LLM.

## Features

- 🤖 **AI Integration** - Support for various LLM APIs (OpenAI compatible)
- 💬 **Chat API** - RESTful chat interface with streaming support
- 🧠 **Agent Core** - Built-in agent with conversation memory
- 🔧 **Tool System** - Function calling support
- 🌊 **Streaming** - Server-Sent Events (SSE) for real-time responses
- 🔄 **CORS Enabled** - Easy frontend integration

## Quick Start

### Prerequisites

- JDK 17+
- Maven 3.8+

### Build

```bash
mvn clean package -DskipTests
```

### Run

```bash
# Set API key
export AI_API_KEY=your-api-key-here

# Run
java -jar target/mini-agent-0.0.1-SNAPSHOT.jar

# Or use Spring Boot
mvn spring-boot:run
```

## Configuration

Edit `src/main/resources/application.yml`:

```yaml
server:
  port: 8080

ai:
  api-key: ${AI_API_KEY}
  base-url: https://api.minimaxi.com/v1
  model: MiniMax-M2.5
  max-tokens: 4096
  temperature: 0.7

agent:
  system-prompt: You are a helpful AI assistant.
  max-history: 10
```

## API Endpoints

### Health Check

```bash
GET /api/health
```

### Chat

```bash
POST /api/chat
Content-Type: application/json

{
  "messages": [
    {"role": "user", "content": "Hello!"}
  ]
}
```

### Stream Chat

```bash
POST /api/chat/stream
Content-Type: application/json

{
  "messages": [
    {"role": "user", "content": "Tell me a story"}
  ]
}
```

## Architecture

```
mini-agent/
├── config/          # Configuration classes
├── controller/      # REST API controllers
├── model/           # Data models
├── service/         # Business services
├── agent/           # Agent core
├── tool/            # Tool system
└── exception/       # Exception handling
```

## License

MIT
