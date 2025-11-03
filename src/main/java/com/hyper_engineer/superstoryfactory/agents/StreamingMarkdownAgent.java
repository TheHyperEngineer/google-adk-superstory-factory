package com.hyper_engineer.superstoryfactory.agents;

import com.google.adk.agents.BaseAgent;
import com.google.adk.agents.LlmAgent;
import org.springframework.stereotype.Component;

@Component
public class StreamingMarkdownAgent {

    public BaseAgent getAgent() {
        return LlmAgent.builder()
                .name("markdown-streaming-agent")
                .description("A helpful assistant that streams responses in Markdown format.")
                .instruction("""
                        You are a helpful AI assistant.
                        Your goal is to provide a detailed and informative response to the user's topic.
                        You MUST format your entire response using Markdown.
                        Use headings, bold text, bullet points, and other Markdown elements to structure your answer clearly.
                        """)
                .model("gemini-2.5-flash")
                .build();
    }
}