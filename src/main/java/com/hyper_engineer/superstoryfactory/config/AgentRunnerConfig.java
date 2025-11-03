package com.hyper_engineer.superstoryfactory.config;

import com.google.adk.runner.InMemoryRunner;
import com.hyper_engineer.superstoryfactory.agents.RouterAgent;
import com.hyper_engineer.superstoryfactory.agents.StoryFactoryAgent;
import com.hyper_engineer.superstoryfactory.agents.StreamingMarkdownAgent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AgentRunnerConfig {

    // This bean is no longer directly used by a controller, but might be useful for testing.
    @Bean
    public InMemoryRunner storyFactoryRunner(StoryFactoryAgent storyFactoryAgent) {
        return new InMemoryRunner(storyFactoryAgent.getAgent());
    }

    // This bean is also no longer directly used.
    @Bean
    public InMemoryRunner streamingChatRunner(StreamingMarkdownAgent streamingMarkdownAgent) {
        return new InMemoryRunner(streamingMarkdownAgent.getAgent());
    }

    // The NEW primary runner for our unified endpoint
    @Bean
    public InMemoryRunner routerRunner(RouterAgent routerAgent) {
        return new InMemoryRunner(routerAgent.getAgent());
    }
}