package com.hyper_engineer.superstoryfactory.controllers;

import com.google.adk.agents.RunConfig;
import com.google.adk.events.Event;
import com.google.adk.runner.InMemoryRunner;
import com.google.adk.sessions.Session;
import com.google.genai.types.Content;
import com.google.genai.types.Part;
import com.hyper_engineer.superstoryfactory.agents.NewsReportAgent;
import io.reactivex.rxjava3.core.Flowable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AgentTestController {

    private final NewsReportAgent newsReportAgent;

    public AgentTestController(NewsReportAgent newsReportAgent) {
        this.newsReportAgent = newsReportAgent;
    }

    @GetMapping("/test-story-writer")
    public String testStoryWriter(@RequestParam String topic) {
        // Use default run configuration
        RunConfig runConfig = RunConfig.builder().build();

        // The InMemoryRunner is the simplest way to execute an agent
        InMemoryRunner runner = new InMemoryRunner(newsReportAgent.getAgent());

        // Create a session for the conversation
        Session session = runner
                .sessionService()
                .createSession(runner.appName(), "test-user")
                .blockingGet();

        // Create the user's message from the topic
        Content userMsg = Content.fromParts(Part.fromText(topic));

        // Run the agent asynchronously and get a stream of events
        Flowable<Event> events = runner.runAsync(session.userId(), session.id(), userMsg, runConfig);

        // Process the event stream to find the final response
        StringBuilder response = new StringBuilder();
        events.blockingForEach(event -> {
            if (event.finalResponse()) {
                response.append(event.stringifyContent());
            }
        });

        return response.toString();
    }
}