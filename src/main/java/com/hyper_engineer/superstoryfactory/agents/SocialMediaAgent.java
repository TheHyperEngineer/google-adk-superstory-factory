package com.hyper_engineer.superstoryfactory.agents;

import com.google.adk.agents.BaseAgent;
import com.google.adk.agents.LlmAgent;
import com.google.adk.tools.GoogleSearchTool;
import org.springframework.stereotype.Component;

@Component
public class SocialMediaAgent {

    public BaseAgent getAgent() {
        return LlmAgent.builder()
                .name("sticker-maker")
                .description("Generates relevant and trending hashtags for a news topic using search.")
                .instruction("""
                        You are a social media engagement expert specializing in SEO. Your goal is to generate 5-7 relevant hashtags for a news story.
                        
                        Follow these steps:
                        1.  First, you MUST use the `google_search` tool to find current information and discussions related to the user's topic.
                        2.  Then, analyze the search results to identify keywords and trending terms.
                        3.  Finally, based on your analysis of the search results, provide a single line of comma-separated hashtags.
                        """)
                .model("gemini-2.5-flash")
                .outputKey("hashtags")
                .tools(new GoogleSearchTool())
                .build();
    }
}