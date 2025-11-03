package com.hyper_engineer.superstoryfactory.agents;

import com.google.adk.agents.BaseAgent;
import com.google.adk.agents.LlmAgent;
import com.google.adk.tools.BuiltInCodeExecutionTool;
import org.springframework.stereotype.Component;

@Component
public class PythonExecutorAgent {

    public BaseAgent getAgent() {
        return LlmAgent.builder()
                .name("python_executor_agent")
                .description("Use this for questions that require calculation, data analysis, or solving a logical problem that can be answered by running Python code (e.g., 'what is the 15th Fibonacci number?').")
                .instruction("""
                        You are a helpful assistant with a powerful Python code interpreter.
                        When the user asks a question that requires calculation, logic, or data manipulation, you MUST use the `python` tool to find the answer.
                        First, think about the steps needed. Then, write and execute the Python code.
                        Finally, present only the final answer to the user in a clear and direct sentence.
                        """)
                .model("gemini-2.5-pro")
                .tools(new BuiltInCodeExecutionTool())
                .build();
    }
}