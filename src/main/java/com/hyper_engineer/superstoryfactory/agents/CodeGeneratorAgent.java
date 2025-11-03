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
                .description("Writes Python code based on a request and feedback.")
                // Use {feedback?} to make the variable optional
                .instruction("You are an expert Python developer. Write a single Python function that satisfies the user's request. If you are given feedback, refine the code to address it.\nPrevious feedback: {feedback?}")
                .model("gemini-2.5-flash")
                .outputKey("generated_code")
                .build();

        // ... rest of the file is unchanged ...
        var testerAgent = LlmAgent.builder()
                .name("tester_agent")
                .description("Writes pytest unit tests for a given Python function.")
                .instruction("You are a QA engineer. Write a set of pytest unit tests for the following Python code. Ensure you cover edge cases. Do not add any explanation, just the code block. Code to test:\n{generated_code}")
                .model("gemini-2.5-flash")
                .outputKey("test_code")
                .build();

        var developmentTeam = ParallelAgent.builder()
                .name("development_team")
                .subAgents(developerAgent, testerAgent)
                .build();

        var reviewerAgent = LlmAgent.builder()
                .name("code_reviewer")
                .description("Reviews code and tests, providing feedback or approval.")
                .instruction("You are a senior software architect. Review the provided Python code and its pytest tests. Check for correctness, style, and completeness. If the code and tests are perfect, you MUST call the `exit_loop` tool with the reason \"Code approved.\" Otherwise, provide constructive feedback for the developer. Code to review:\n{generated_code}\n\nTests to review:\n{test_code}")
                .model("gemini-2.5-flash")
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
                .description("Presents the final code and tests to the user.")
                .instruction("The code has been successfully generated, tested, and reviewed. Present the final version of the code and the tests to the user in a clear, well-formatted Markdown response. Use separate fenced code blocks for the Python function and the tests.\n\nFinal Code:\n{generated_code}\n\nFinal Tests:\n{test_code}")
                .model("gemini-2.5-flash")
                .build();

        return SequentialAgent.builder()
                .name("code_generator_agent")
                .description("Manages the full software development lifecycle: writing code, creating tests, reviewing, and presenting the final result. Use for any request involving writing code or functions.")
                .subAgents(reviewCycle, presenterAgent)
                .build();
    }
}