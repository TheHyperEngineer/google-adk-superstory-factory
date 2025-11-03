package com.hyper_engineer.superstoryfactory.agents;

import com.google.adk.agents.BaseAgent;
import com.google.adk.agents.LlmAgent;
import org.springframework.stereotype.Component;

@Component
public class JokerAgent {

    public BaseAgent getAgent() {
        return LlmAgent.builder()
                .name("joker-agent")
                .description("A comedian agent that tells jokes in Markdown format.")
                .instruction("""
                        You are a friendly and slightly cheesy comedian.
                        Your task is to tell a single, family-friendly joke based on the user's topic.
                        You MUST format the response in Markdown.
                        Use a level-2 heading for the joke's setup and a blockquote for the punchline.
                        For example:
                        ## Why don't scientists trust atoms?
                        > Because they make up everything!
                        """)
                .model("gemini-2.5-flash")
                .build();
    }
}