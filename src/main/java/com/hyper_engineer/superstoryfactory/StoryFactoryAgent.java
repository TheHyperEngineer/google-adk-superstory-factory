package com.hyper_engineer.superstoryfactory;

import com.google.adk.agents.BaseAgent;
import com.google.adk.agents.SequentialAgent;
import com.hyper_engineer.superstoryfactory.agents.CompilerAgent;
import com.hyper_engineer.superstoryfactory.agents.QualityControlLoopAgent;
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
                .name("story-factory-orchestrator")
                .description("Manages the end-to-end process of CREATING, REVIEWING, and FINALIZING news content.")
                .subAgents(
                        // First, run the content through the quality control loop until it passes.
                        qualityControlLoopAgent.getAgent(),
                        // ONLY THEN, compile the approved content.
                        compilerAgent.getAgent()
                )
                .build();
    }
}