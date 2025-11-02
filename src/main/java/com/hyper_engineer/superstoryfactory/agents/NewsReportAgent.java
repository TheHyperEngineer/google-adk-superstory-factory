package com.hyper_engineer.superstoryfactory.agents;

import com.google.adk.agents.BaseAgent;
import com.google.adk.agents.LlmAgent;
import org.springframework.stereotype.Component;

@Component
public class NewsReportAgent {

    public BaseAgent getAgent() {
        return LlmAgent.builder()
                .name("story-writer")
                .description("Writes a news article based on a given topic.")
                .instruction("""
                        You are a professional news journalist.
                        Your task is to write a compelling, well-structured news article about the provided topic.
                        The article must have a clear headline, an introductory paragraph, a body with supporting details, and a concluding paragraph.
                        """)
                .model("gemini-2.5-flash") // Using a standard, available model
                .outputKey("news_report") // Crucial for chaining agents later
                .build();
    }
}
