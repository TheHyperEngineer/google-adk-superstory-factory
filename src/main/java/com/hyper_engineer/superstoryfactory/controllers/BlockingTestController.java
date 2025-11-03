package com.hyper_engineer.superstoryfactory.controllers;

import com.google.adk.agents.RunConfig;
import com.google.adk.events.Event;
import com.google.adk.runner.InMemoryRunner;
import com.google.adk.sessions.Session;
import com.google.genai.types.Content;
import com.google.genai.types.Part;
import com.hyper_engineer.superstoryfactory.agents.JokerAgent;
import io.reactivex.rxjava3.core.Flowable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BlockingTestController {

    private static final Logger log = LoggerFactory.getLogger(BlockingTestController.class);
    private final JokerAgent jokerAgent;

    public BlockingTestController(JokerAgent jokerAgent) {
        this.jokerAgent = jokerAgent;
    }

    @GetMapping(value = "/test-joke", produces = "text/markdown")
    public String tellJoke(@RequestParam String topic) {
        log.info("Received blocking joke request for topic: {}", topic);
        StringBuilder response = new StringBuilder();

        try {
            // For this simple, blocking use case, creating a local runner is acceptable.
            InMemoryRunner runner = new InMemoryRunner(jokerAgent.getAgent());
            RunConfig runConfig = RunConfig.builder().build();

            Session session = runner.sessionService()
                    .createSession(runner.appName(), "test-user")
                    .blockingGet();

            Content userMsg = Content.fromParts(Part.fromText(topic));
            Flowable<Event> events = runner.runAsync(session.userId(), session.id(), userMsg, runConfig);

            // This is the key: .blockingForEach() makes the call synchronous.
            // The controller thread will wait here until the agent is finished.
            events.blockingForEach(event -> {
                log.debug("[Joke Event] Author: {}, Final: {}", event.author(), event.finalResponse());
                if (event.finalResponse()) {
                    response.append(event.stringifyContent());
                }
            });

            if (response.isEmpty()) {
                log.warn("Joker agent for topic '{}' finished but produced no final response.", topic);
                return "## No Joke Found\n\nSorry, my wit has failed me. I couldn't think of a joke about that.";
            }

            log.info("Successfully generated joke for topic: {}", topic);
            return response.toString();

        } catch (Exception e) {
            log.error("An error occurred while generating a joke for topic '{}': {}", topic, e.getMessage(), e);
            // Return a user-friendly error in Markdown format
            return "## Error\n\nSorry, I short-circuited while trying to be funny. Please check the server logs.";
        }
    }
}