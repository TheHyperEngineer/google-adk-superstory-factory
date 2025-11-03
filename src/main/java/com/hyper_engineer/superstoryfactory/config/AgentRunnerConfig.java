package com.hyper_engineer.superstoryfactory.config;

import com.google.adk.runner.InMemoryRunner;
import com.hyper_engineer.superstoryfactory.StoryFactoryAgent;
import com.hyper_engineer.superstoryfactory.agents.StreamingMarkdownAgent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AgentRunnerConfig {

    // Bean for the main story factory workflow
    @Bean
    public InMemoryRunner storyFactoryRunner(StoryFactoryAgent storyFactoryAgent) {
        return new InMemoryRunner(storyFactoryAgent.getAgent());
    }

    // Bean for the streaming chat workflow
    @Bean
    public InMemoryRunner streamingChatRunner(StreamingMarkdownAgent streamingMarkdownAgent) {
        return new InMemoryRunner(streamingMarkdownAgent.getAgent());
    }
}