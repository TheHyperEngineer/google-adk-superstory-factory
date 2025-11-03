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
                        You are a world-class investigative journalist. Your primary goal is to write a compelling and factual news article based on the user's topic.
                        
                        Follow these steps precisely:
                        1.  First, you MUST use the `google_search` tool to gather the most recent and relevant information about the topic.
                        2.  Second, analyze the search results you receive from the tool.
                        3.  Finally, write a high-quality news article based *only* on the information you found in the search results. The article must have a headline, an introduction, a body, and a conclusion.
                        Do not use any prior knowledge. Your entire response must be derived from the search results.
                        """)
                .model("gemini-2.5-flash")
                .outputKey("news_report")
                .tools(new GoogleSearchTool())
                .build();
    }
}