package com.hyper_engineer.superstoryfactory.config;

import com.google.adk.runner.InMemoryRunner;
import com.hyper_engineer.superstoryfactory.agents.RouterAgent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AgentRunnerConfig {

    // This is the only runner needed for the application.
    // It creates an instance of our master RouterAgent.
    @Bean
    public InMemoryRunner routerRunner(RouterAgent routerAgent) {
        return new InMemoryRunner(routerAgent.getAgent());
    }
}