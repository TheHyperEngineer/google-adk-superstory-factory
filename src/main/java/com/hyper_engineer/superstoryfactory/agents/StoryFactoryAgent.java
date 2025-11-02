package com.hyper_engineer.superstoryfactory.agents;

import com.google.adk.agents.BaseAgent;
import com.google.adk.agents.SequentialAgent;
import org.springframework.stereotype.Component;

@Component
public class StoryFactoryAgent {

    private final NewsDeskAgent newsDeskAgent;
    private final CompilerAgent compilerAgent;

    public StoryFactoryAgent(NewsDeskAgent newsDeskAgent, CompilerAgent compilerAgent) {
        this.newsDeskAgent = newsDeskAgent;
        this.compilerAgent = compilerAgent;
    }

    public BaseAgent getAgent() {
        return SequentialAgent.builder()
                .name("story-factory-orchestrator")
                .description("Manages the end-to-end process of news content creation.")
                .subAgents(
                        newsDeskAgent.getAgent(),
                        compilerAgent.getAgent()
                )
                .build();
    }
}