package com.hyper_engineer.superstoryfactory.agents;

import com.google.adk.agents.BaseAgent;
import com.google.adk.agents.LlmAgent;
import com.google.adk.agents.LoopAgent;
import com.google.adk.agents.ParallelAgent;
import com.google.adk.agents.SequentialAgent;
import com.google.adk.tools.ExitLoopTool;
import org.springframework.stereotype.Component;

@Component
public class CodeGeneratorAgent {

    public BaseAgent getAgent() {
        var developerAgent = LlmAgent.builder()
                .name("developer_agent")
                .instruction("You are an expert Python developer. Write a single Python function that satisfies the user's request. Do not add any explanation, just the code block. If you are given feedback, refine the code to address it. Previous feedback: {feedback}")
                .model("gemini-1.5-pro")
                .outputKey("generated_code")
                .build();

        var testerAgent = LlmAgent.builder()
                .name("tester_agent")
                .instruction("You are a QA engineer. Write a set of pytest unit tests for the following Python code. Ensure you cover edge cases. Do not add any explanation, just the code block. Code to test:\n{generated_code}")
                .model("gemini-1.5-pro")
                .outputKey("test_code")
                .build();

        var developmentTeam = ParallelAgent.builder()
                .name("development_team")
                .subAgents(developerAgent, testerAgent)
                .build();

        var reviewerAgent = LlmAgent.builder()
                .name("code_reviewer")
                .instruction("You are a senior software architect. Review the provided Python code and its pytest tests. Check for correctness, style, and completeness. If the code and tests are perfect, you MUST call the `exit_loop` tool with the reason \"Code approved.\" Otherwise, provide constructive feedback for the developer. Code to review:\n{generated_code}\n\nTests to review:\n{test_code}")
                .model("gemini-1.5-pro")
                .outputKey("feedback")
                .tools(ExitLoopTool.INSTANCE)
                .build();

        var reviewCycle = LoopAgent.builder()
                .name("review_cycle")
                .subAgents(developmentTeam, reviewerAgent)
                .maxIterations(3)
                .build();

        var presenterAgent = LlmAgent.builder()
                .name("presenter_agent")
                .instruction("The code has been successfully generated, tested, and reviewed. Present the final version of the code and the tests to the user in a clear, well-formatted Markdown response. Use separate fenced code blocks for the Python function and the tests.\n\nFinal Code:\n{generated_code}\n\nFinal Tests:\n{test_code}")
                .model("gemini-2.5-flash")
                .build();

        return SequentialAgent.builder()
                .name("code_generator_agent")
                .description("Use this for requests that involve writing a function, class, or block of code in any programming language (e.g., 'write a python function to...').")
                .subAgents(reviewCycle, presenterAgent)
                .build();
    }
}