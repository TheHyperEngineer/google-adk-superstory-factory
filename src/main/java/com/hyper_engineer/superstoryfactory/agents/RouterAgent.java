package com.hyper_engineer.superstoryfactory.agents;

import com.google.adk.agents.BaseAgent;
import com.google.adk.agents.LlmAgent;
import com.google.adk.tools.AgentTool;
import org.springframework.stereotype.Component;

@Component
public class RouterAgent {

    private final StoryFactoryAgent storyFactoryAgent;
    private final StreamingMarkdownAgent streamingMarkdownAgent;
    private final JokerAgent jokerAgent;

    public RouterAgent(
            StoryFactoryAgent storyFactoryAgent,
            StreamingMarkdownAgent streamingMarkdownAgent,
            JokerAgent jokerAgent) {
        this.storyFactoryAgent = storyFactoryAgent;
        this.streamingMarkdownAgent = streamingMarkdownAgent;
        this.jokerAgent = jokerAgent;
    }

    public BaseAgent getAgent() {
        var storyFactoryTool = AgentTool.create(storyFactoryAgent.getAgent());
        var jokerTool = AgentTool.create(jokerAgent.getAgent());

        // We will handle the default case in the controller, so the router only needs to focus on special cases.
        return LlmAgent.builder()
                .name("master-router-agent")
                .description("The central router that directs user requests to specialist agents.")
                .instruction("""
                        You are a routing agent. Your job is to analyze the user's request and determine if it requires a specialized tool.
                        
                        You have the following specialized tools:
                        - 'story_factory': Use this ONLY for explicit requests to "write a story", "create a news report", or "generate an article".
                        - 'joke_teller': Use this ONLY for requests that explicitly ask for a "joke".
                        
                        If the user's request does NOT match any of the specialized tools (e.g., it's a greeting, a general question, or a simple conversation),
                        DO NOT call any tool. Instead, simply respond with the text "DEFAULT".
                        """)
                .model("gemini-2.5-flash") // We can use a faster model for this simpler routing task.
                .tools(storyFactoryTool, jokerTool)
                .build();
    }

    // Expose the default agent so the controller can use it
    public BaseAgent getDefaultAgent() {
        return streamingMarkdownAgent.getAgent();
    }
}