package com.hyper_engineer.superstoryfactory.agents;

import com.google.adk.agents.BaseAgent;
import com.google.adk.agents.LlmAgent;
import com.google.adk.tools.AgentTool;
import org.springframework.stereotype.Component;

@Component
public class RouterAgent {

    private final StoryFactoryAgent storyFactoryAgent;
    private final JokerAgent jokerAgent;
    private final StockTickerAgent stockTickerAgent;
    private final CodeGeneratorAgent codeGeneratorAgent;
    private final PythonExecutorAgent pythonExecutorAgent;

    public RouterAgent(
            StoryFactoryAgent storyFactoryAgent,
            JokerAgent jokerAgent,
            StockTickerAgent stockTickerAgent,
            CodeGeneratorAgent codeGeneratorAgent,
            PythonExecutorAgent pythonExecutorAgent) {
        this.storyFactoryAgent = storyFactoryAgent;
        this.jokerAgent = jokerAgent;
        this.stockTickerAgent = stockTickerAgent;
        this.codeGeneratorAgent = codeGeneratorAgent;
        this.pythonExecutorAgent = pythonExecutorAgent;
    }

    public BaseAgent getAgent() {
        // Create tools from our agents. The descriptions are correctly read from the agent definitions.
        var storyFactoryTool = AgentTool.create(storyFactoryAgent.getAgent());
        var jokerTool = AgentTool.create(jokerAgent.getAgent());
        var stockTickerTool = AgentTool.create(stockTickerAgent.getAgent());
        var codeGeneratorTool = AgentTool.create(codeGeneratorAgent.getAgent());
        var pythonExecutorTool = AgentTool.create(pythonExecutorAgent.getAgent());

        return LlmAgent.builder()
                .name("master-router-agent")
                .description("The primary conversational agent and router.")
                .instruction("""
                        You are the primary interface for a powerful AI system. Your first priority is to determine if the user's request requires a specialized tool.

                        Here are the available tools:
                        - 'story_factory': Use for explicit requests to "write a story", "create a news report", or "generate an article".
                        - 'joke_teller': Use for requests that explicitly ask for a "joke".
                        - 'stock_ticker_agent': Use for requests about the price of a stock, identified by a ticker symbol (e.g., GOOG, AAPL).
                        - 'code_generator_agent': Use for requests to write a function, class, or block of code.
                        - 'python_executor_agent': Use for questions that require calculation, logic, or data analysis that can be solved with Python.

                        **If the user's request matches one of the tools, you MUST call that tool.** The tool will provide the complete answer.

                        **If the user's request does NOT match any tool (e.g., it's a greeting, a general knowledge question like 'what is the capital of France?', or a simple conversation), you MUST answer the user's question directly and helpfully yourself.** Format your answer in Markdown.
                        """)
                .model("gemini-2.5-pro")
                .tools(storyFactoryTool, jokerTool, stockTickerTool, codeGeneratorTool, pythonExecutorTool)
                .build();
    }
}