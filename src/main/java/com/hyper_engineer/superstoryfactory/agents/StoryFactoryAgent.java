package com.hyper_engineer.superstoryfactory.agents;

import com.google.adk.agents.BaseAgent;
import com.google.adk.agents.SequentialAgent;
import org.springframework.stereotype.Component;

@Component
public class StoryFactoryAgent {

    // The NewsDeskAgent is no longer a direct dependency here.
    // It is now managed by the QualityControlLoopAgent.
    private final QualityControlLoopAgent qualityControlLoopAgent;
    private final CompilerAgent compilerAgent;

    public StoryFactoryAgent(QualityControlLoopAgent qualityControlLoopAgent, CompilerAgent compilerAgent) {
        this.qualityControlLoopAgent = qualityControlLoopAgent;
        this.compilerAgent = compilerAgent;
    }

    public BaseAgent getAgent() {
        return SequentialAgent.builder()
                .name("story_factory") // Name matches the tool name
                // This description is now critical for the router
                .description("Use this tool to generate a full news report, including a story, tweets, and hashtags on a specific topic. This is a comprehensive, multi-step process.")
                .subAgents(
                        qualityControlLoopAgent.getAgent(),
                        compilerAgent.getAgent()
                )
                .build();
    }

}