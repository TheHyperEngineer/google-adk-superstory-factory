package com.hyper_engineer.superstoryfactory.agents;

import com.google.adk.agents.BaseAgent;
import com.google.adk.agents.LlmAgent;
import com.google.adk.tools.GoogleSearchTool;
import org.springframework.stereotype.Component;

@Component
public class NewsReportAgent {

    public BaseAgent getAgent() {
        return LlmAgent.builder()
                .name("story-writer")
                .description("Writes a news article based on a given topic, using real-time search.")
                .instruction("""
                        You are a world-class investigative journalist.
                        Your task is to write a compelling, well-structured news article about the provided topic.
                        **Crucially, you MUST use the `google_search` tool to find the most recent, up-to-date information on the topic before you start writing.**
                        Base your article on the search results to ensure it is timely and accurate.
                        The article must have a clear headline, an introductory paragraph, a body with supporting details, and a concluding paragraph.
                        """)
                .model("gemini-2.5-flash")
                .outputKey("news_report")
                .tools(new GoogleSearchTool()) // Add the Google Search tool to this agent
                .build();
    }
}