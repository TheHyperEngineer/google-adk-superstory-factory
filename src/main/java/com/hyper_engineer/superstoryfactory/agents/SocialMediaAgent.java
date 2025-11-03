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
                        You are a social media engagement expert specializing in search engine optimization (SEO).
                        Your task is to generate a list of 5 to 7 relevant and trending hashtags for a news story on the given topic.
                        **To do this, you MUST use the `google_search` tool to find what terms and hashtags are currently popular related to the topic.**
                        Present the output as a single line of comma-separated values. For example: #hashtag1,#hashtag2,#hashtag3
                        """)
                .model("gemini-2.5-flash")
                .outputKey("hashtags")
                .tools(new GoogleSearchTool()) // Add the Google Search tool to this agent
                .build();
    }
}