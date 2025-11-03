***

## 1. Technical README for the Backend (Super Story Factory API)

```markdown
# Super Story Factory API - Technical Documentation

## 1. Overview

The Super Story Factory is a sophisticated, multi-agent AI content generation platform built with Java, Spring Boot, and
Google's Agent Development Kit (ADK). It is designed as a microservice that exposes a unified, intelligent
conversational API capable of routing user requests to specialized AI agents for tasks such as news article generation,
joke telling, and general chat.

The architecture emphasizes modularity, scalability, and resilience, employing advanced agentic design patterns like
supervisor-worker, critique-and-refine loops, and tool-assisted generation.

### Core Technologies

* **Java 21+**
* **Spring Boot 3.x** (with WebFlux for reactive, non-blocking I/O)
* **Google ADK 0.3.0** (for agent creation and orchestration)
* **Google Gemini API** (for Large Language Model access)
* **Maven** (for dependency management)

## 2. Architecture

The application follows a modular, agent-oriented design pattern.

![Architecture Diagram](https://i.imgur.com/9g8f7hC.png)

### Key Components:

* **`ChatController`**: The single entry point for all user interactions. It exposes a reactive, Server-Sent Events (
  SSE) endpoint at `/api/chat`.
* **`AgentRunnerConfig`**: A Spring Configuration class that instantiates and manages the lifecycle of `InMemoryRunner`
  objects for each primary agent, ensuring they are treated as singletons.
* **`RouterAgent`**: The master orchestrator. This `LlmAgent` receives the initial user prompt and decides which
  specialized sub-agent to delegate the task to. It uses the "agent-as-a-tool" pattern.
* **Specialist Agents**:
    * **`StoryFactoryAgent`**: A `SequentialAgent` that manages a complex workflow for generating high-quality news
      articles.
        * **`QualityControlLoopAgent`**: A `LoopAgent` that forces an iterative review process. It contains the
          `NewsDeskAgent` and the `CriticAgent`.
        * **`NewsDeskAgent`**: A `ParallelAgent` that executes the `NewsReportAgent`, `TweetAgent`, and
          `SocialMediaAgent` concurrently for efficiency.
        * **`CriticAgent`**: An `LlmAgent` that evaluates the generated content and either approves it (by calling
          `ExitLoopTool`) or provides feedback for revision.
        * **`CompilerAgent`**: An `LlmAgent` that formats the final, approved content into a structured JSON object.
    * **`StreamingMarkdownAgent`**: An `LlmAgent` that serves as the default handler for general conversation, providing
      streaming, Markdown-formatted responses.
    * **`JokerAgent`**: A simple `LlmAgent` for a specific, fun task.
* **Tools**:
    * **`GoogleSearchTool`**: Used by the `NewsReportAgent` and `SocialMediaAgent` to fetch real-time information from
      the web, grounding their responses in current events.
    * **`AgentTool`**: Used by the `RouterAgent` to wrap other agents, making them callable as functions.
    * **`ExitLoopTool`**: Used by the `CriticAgent` to signal the successful completion of the quality control loop.

## 3. Setup and Installation

### Prerequisites

* Java JDK 21 or higher
* Apache Maven 3.6+
* Access to the Google AI Gemini API

### Environment Configuration

The application requires three environment variables to be set before running:

1. **`GEMINI_API_KEY`**: Your API key for the Google Gemini models. This is used for the core LLM calls.
2. **`GOOGLE_API_KEY`**: Your Google Cloud API key. This can be the same as your Gemini key and is required by the
   `GoogleSearchTool`.
3. **`GOOGLE_CSE_ID`**: Your Custom Search Engine ID. To get one:
    * Go to the [Programmable Search Engine control panel](https://programmablesearch.google.com/controlpanel/all).
    * Create a new search engine.
    * Configure it to "Search the entire web".
    * Copy the "Search engine ID" from the control panel.

**Example (Linux/macOS):**

```bash
export GEMINI_API_KEY="your_api_key_here"
export GOOGLE_API_KEY="your_api_key_here"
export GOOGLE_CSE_ID="your_search_engine_id_here"
```

**Example (Windows PowerShell):**

```powershell
$env:GEMINI_API_KEY="your_api_key_here"
$env:GOOGLE_API_KEY="your_api_key_here"
$env:GOOGLE_CSE_ID="your_search_engine_id_here"
```

### Building and Running

1. Clone the repository.
2. Navigate to the project's root directory.
3. Compile and run the application using the Maven wrapper:

   ```bash
   ./mvnw spring-boot:run
   ```

The server will start on `http://localhost:8080`.

## 4. API Endpoints

The application exposes one primary endpoint and one for testing.

### Unified Chat Endpoint (SSE)

This is the main endpoint for all interactions. It provides a streaming response.

* **URL:** `/api/chat`
* **Method:** `GET`
* **Produces:** `text/event-stream`
* **Query Parameter:**
    * `prompt` (string, required): The user's message or request.

**Example Usage (curl):**

```bash
# For a general chat question
curl -N "http://localhost:8080/api/chat?prompt=What%20is%20the%20capital%20of%20France"

# To request a news story
curl -N "http://localhost:8080/api/chat?prompt=Write%20a%20news%20story%20about%20the%20latest%20Mars%20rover"

# To request a joke
curl -N "http://localhost:8080/api/chat?prompt=Tell%20me%20a%20joke%20about%20coffee"
```

### Blocking Test Endpoint

A simple, synchronous endpoint for quick verification of the `JokerAgent`.

* **URL:** `/test-joke`
* **Method:** `GET`
* **Produces:** `text/markdown`
* **Query Parameter:**
    * `topic` (string, required): The topic for the joke.

**Example Usage (curl):**

```bash
curl http://localhost:8080/test-joke?topic=computers
```

## 5. How to Extend

* **Adding a New Skill/Agent:**
    1. Create a new agent class (e.g., `PoemAgent.java`) in the `agents` package.
    2. Define its logic using `LlmAgent` or other ADK constructs.
    3. Add the new agent as a bean in `AgentRunnerConfig.java`.
    4. In `RouterAgent.java`, create a new `AgentTool` from your new agent.
    5. Update the `RouterAgent`'s prompt to include instructions on when to use this new tool.

* **Adding a New Tool:**
    1. Create a new Java class with a public static method.
    2. Annotate the method and its parameters with `@Schema` from the ADK library.
    3. In the agent where you want to use the tool, add it to the builder:
       `.tools(FunctionTool.create(YourToolClass.class, "yourMethodName"))`.
    4. Update the agent's prompt to instruct it on how and when to use the new tool.

```