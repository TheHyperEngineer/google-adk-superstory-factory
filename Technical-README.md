# Super Story Factory API - Technical Documentation

## 1. Overview

The Super Story Factory is a sophisticated, multi-agent AI content generation platform built with Java, Spring Boot, and Google's Agent Development Kit (ADK). It is designed as a microservice that exposes a unified, intelligent conversational API capable of routing user requests to specialized AI agents for tasks such as news article generation, code generation, financial data lookups, and general chat.

The architecture emphasizes modularity, scalability, and resilience, employing advanced agentic design patterns like supervisor-worker, critique-and-refine loops, and tool-assisted generation.

### Core Technologies

*   **Java 21+**
*   **Spring Boot 3.x** (with WebFlux for reactive endpoints)
*   **Google ADK 0.3.0** (for agent creation and orchestration)
*   **Google Gemini API** (for Large Language Model access)
*   **Maven** (for dependency management)

## 2. Architecture

The application is centered around a powerful **Router Agent** that acts as a central dispatcher. This design allows for a clean separation of concerns and makes the system highly extensible.

![Architecture Diagram](https://i.imgur.com/L3y2a1g.png)

### Key Components:

*   **Controllers**:
    *   **`ChatController`**: The primary entry point for all user interactions. It exposes a reactive, Server-Sent Events (SSE) endpoint at `/api/chat`. This controller is responsible for handling general conversation and streaming responses.
    *   **`AgentExecutionController`**: A non-streaming, asynchronous endpoint at `/api/execute` designed to handle complex, multi-step agent tasks that produce a single, final result (e.g., generating a news article or writing code).

*   **Agent Runners (`AgentRunnerConfig`)**:
    *   **`routerRunner`**: A singleton `InMemoryRunner` that executes the `RouterAgent` to handle requests for specialized tasks.
    *   **`streamingChatRunner`**: A singleton `InMemoryRunner` that executes the `StreamingMarkdownAgent` for general-purpose, streaming chat.

*   **Core Agents**:
    *   **`RouterAgent`**: The master orchestrator. This `LlmAgent` analyzes the user's prompt and calls the appropriate specialized agent as a tool. It is the entry point for the `/api/execute` endpoint.
    *   **`StreamingMarkdownAgent`**: The default conversational agent. It handles all requests not meant for a specialist, providing streaming, Markdown-formatted responses. It is the primary agent for the `/api/chat` endpoint.

*   **Specialist Agents (used as Tools by the Router)**:
    *   **`StoryFactoryAgent`**: A complex `SequentialAgent` that manages a multi-step workflow (including a `LoopAgent` for critique and refinement) to generate high-quality news articles, complete with tweets and hashtags.
    *   **`CodeGeneratorAgent`**: A `SequentialAgent` that orchestrates a team of sub-agents (`developer`, `tester`, `reviewer`) in a `LoopAgent` to write, test, and refine code based on a user's request.
    *   **`PythonExecutorAgent`**: An `LlmAgent` equipped with the `BuiltInCodeExecutionTool`, allowing it to execute Python code to solve computational or logical problems.
    *   **`StockTickerAgent`**: An `LlmAgent` that uses a custom `FunctionTool` to fetch (mock) stock price information.
    *   **`JokerAgent`**: A simple `LlmAgent` for telling jokes.

## 3. Setup and Installation

### Prerequisites

*   Java JDK 21 or higher
*   Apache Maven 3.6+
*   Access to the Google AI Gemini API

### Environment Configuration

The application requires three environment variables to be set before running:

1.  **`GEMINI_API_KEY`**: Your API key for the Google Gemini models. This is used for all LLM calls.
2.  **`GOOGLE_API_KEY`**: Your Google Cloud API key. This can be the same as your Gemini key and is required by the `GoogleSearchTool` used in the `StoryFactoryAgent`.
3.  **`GOOGLE_CSE_ID`**: Your Custom Search Engine ID. To get one:
    *   Go to the [Programmable Search Engine control panel](https://programmablesearch.google.com/controlpanel/all).
    *   Create a new search engine, configure it to "Search the entire web".
    *   Copy the "Search engine ID" from the control panel.

**Example (Linux/macOS):**
```bash
export GEMINI_API_KEY="YOUR_API_KEY_HERE"
export GOOGLE_API_KEY="YOUR_API_KEY_HERE"
export GOOGLE_CSE_ID="YOUR_SEARCH_ENGINE_ID_HERE"
```

**Example (Windows PowerShell):**
```powershell
$env:GEMINI_API_KEY="YOUR_API_KEY_HERE"
$env:GOOGLE_API_KEY="YOUR_API_KEY_HERE"
$env:GOOGLE_CSE_ID="YOUR_SEARCH_ENGINE_ID_HERE"
```

### Building and Running

1.  Clone the repository.
2.  Navigate to the project's root directory.
3.  Compile and run the application using the Maven wrapper:

    ```bash
    ./mvnw spring-boot:run
    ```
The server will start on `http://localhost:8080`.

## 4. API Endpoints

The application now exposes two primary endpoints to cleanly separate streaming and non-streaming tasks.

### A) Streaming Chat Endpoint

For general conversation with a typewriter-style streaming response.

*   **URL:** `/api/chat`
*   **Method:** `GET`
*   **Produces:** `text/event-stream`
*   **Query Parameter:**
    *   `prompt` (string, required): The user's message or question.

**Example Usage (curl):**
```bash
curl -N "http://localhost:8080/api/chat?prompt=What%20is%20photosynthesis"
```

### B) Agent Execution Endpoint

For complex, multi-step tasks that return a single, complete result. This endpoint uses the `RouterAgent` to delegate to the appropriate specialist.

*   **URL:** `/api/execute`
*   **Method:** `GET`
*   **Produces:** `text/plain` (but the content can be Markdown or JSON)
*   **Query Parameter:**
    *   `prompt` (string, required): The user's specific command.

**Example Usage (curl):**
```bash
# Request a news story (returns a JSON string)
curl "http://localhost:8080/api/execute?prompt=Write%20a%20news%20story%20about%20the%20latest%20Mars%20rover"

# Request a joke (returns Markdown)
curl "http://localhost:8080/api/execute?prompt=Tell%20me%20a%20joke%20about%20coffee"

# Request code generation (returns Markdown with code blocks)
curl "http://localhost:8080/api/execute?prompt=write%20a%20python%20function%20to%20calculate%20a%20factorial"
```

## 5. How to Extend

*   **Adding a New Specialist Agent:**
    1.  Create your new agent class (e.g., `RecipeAgent.java`) in the `agents` package and mark it as a `@Component`.
    2.  Define its name and description in the agent's builder (e.g., `.name("recipe_agent").description("Use this to find recipes for food.")`).
    3.  Inject your new agent into the `RouterAgent`'s constructor.
    4.  In `RouterAgent.java`, create a new `AgentTool` from your agent (`var recipeTool = AgentTool.create(recipeAgent.getAgent());`).
    5.  Add the new tool to the `RouterAgent`'s `.tools(...)` list.
    6.  Update the `RouterAgent`'s instruction prompt to explain when this new tool should be used.
```