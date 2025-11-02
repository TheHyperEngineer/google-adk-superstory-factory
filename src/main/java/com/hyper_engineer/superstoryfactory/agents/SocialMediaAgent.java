package com.hyper_engineer.superstoryfactory.agents;

import com.google.adk.agents.BaseAgent;
import com.google.adk.agents.LlmAgent;
import org.springframework.stereotype.Component;

@Component
public class SocialMediaAgent {

    public BaseAgent getAgent() {

        return LlmAgent.builder()
                .name("sticker-maker")
                .description("Generates relevant hashtags for a news topic.")
                .instruction("""
                        You are a social media engagement expert.
                        Your task is to generate a list of 5 to 7 relevant and trending hashtags for a news story on the given topic.
                        Present the output as a single line of comma-separated values. For example: #hashtag1,#hashtag2,#hashtag3
                        """)
                .model("gemini-2.5-flash")
                .outputKey("hashtags")
                .build();
    }
}