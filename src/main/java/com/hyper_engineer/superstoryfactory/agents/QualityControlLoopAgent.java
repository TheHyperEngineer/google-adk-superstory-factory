package com.hyper_engineer.superstoryfactory.agents;

import com.google.adk.agents.BaseAgent;
import com.google.adk.agents.LoopAgent;
import org.springframework.stereotype.Component;

@Component
public class QualityControlLoopAgent {

    private final NewsDeskAgent newsDeskAgent;
    private final CriticAgent criticAgent;

    public QualityControlLoopAgent(NewsDeskAgent newsDeskAgent, CriticAgent criticAgent) {
        this.newsDeskAgent = newsDeskAgent;
        this.criticAgent = criticAgent;
    }

    public BaseAgent getAgent() {
        return LoopAgent.builder()
                .name("quality-control-loop")
                .description("Manages the iterative process of content creation and review.")
                .subAgents(
                        newsDeskAgent.getAgent(),
                        criticAgent.getAgent()
                )
                .maxIterations(3) // A safety net to prevent infinite loops in case of persistent failure
                .build();
    }
}