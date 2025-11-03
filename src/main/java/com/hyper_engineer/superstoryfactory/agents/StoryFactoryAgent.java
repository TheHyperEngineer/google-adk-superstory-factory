package com.hyper_engineer.superstoryfactory.agents;

import com.google.adk.agents.BaseAgent;
import com.google.adk.agents.SequentialAgent;
import org.springframework.stereotype.Component;

@Component
public class StoryFactoryAgent {

    private final QualityControlLoopAgent qualityControlLoopAgent;
    private final CompilerAgent compilerAgent;

    public StoryFactoryAgent(QualityControlLoopAgent qualityControlLoopAgent, CompilerAgent compilerAgent) {
        this.qualityControlLoopAgent = qualityControlLoopAgent;
        this.compilerAgent = compilerAgent;
    }

    public BaseAgent getAgent() {
        return SequentialAgent.builder()
                .name("story_factory")
                .description("Use this for requests to 'write a story', 'create a news report', or 'generate an article'.")
                .subAgents(
                        qualityControlLoopAgent.getAgent(),
                        compilerAgent.getAgent()
                )
                .build();
    }
}