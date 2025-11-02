package com.hyper_engineer.superstoryfactory.agents;

import com.google.adk.agents.BaseAgent;
import com.google.adk.agents.LlmAgent;
import org.springframework.stereotype.Component;

@Component
public class TweetAgent {

    public BaseAgent getAgent() {
        return LlmAgent.builder()
                .name("tweet-writer")
                .description("Creates engaging tweets about a news topic.")
                .instruction("""
                        You are a social media manager for a major news organization.
                        Based on the provided topic, write two short, engaging tweets.
                        Each tweet must be under 280 characters.
                        """)
                .model("gemini-2.5-flash")
                .outputKey("tweets")
                .build();
    }
}