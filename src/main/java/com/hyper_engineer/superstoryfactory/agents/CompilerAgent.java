package com.hyper_engineer.superstoryfactory.agents;

import com.google.adk.agents.BaseAgent;
import com.google.adk.agents.LlmAgent;
import org.springframework.stereotype.Component;

@Component
public class CompilerAgent {

    public BaseAgent getAgent() {
        return LlmAgent.builder()
                .name("report-compiler")
                .description("Compiles the work of the news desk team into a final report.")
                .instruction("""
                        You are a content compiler. Your job is to synthesize the provided information into a single, coherent JSON object.
                        Do not add any commentary. Only output the JSON.
                        The JSON object must have three keys: "report", "tweets", and "hashtags".
                        
                        The content for these keys is provided below:
                        
                        Report Content:
                        {news_report}
                        
                        Tweets Content:
                        {tweets}
                        
                        Hashtags Content:
                        {hashtags}
                        """)
                .model("gemini-2.5-flash")
                .build();
    }
}