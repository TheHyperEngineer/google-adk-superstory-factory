package com.hyper_engineer.superstoryfactory.controllers;

import com.google.adk.agents.RunConfig;
import com.google.adk.events.Event;
import com.google.adk.runner.InMemoryRunner;
import com.google.adk.sessions.Session;
import com.google.genai.types.Content;
import com.google.genai.types.Part;
import com.hyper_engineer.superstoryfactory.agents.StoryFactoryAgent;
import com.hyper_engineer.superstoryfactory.controllers.dto.StoryRequest;
import io.reactivex.rxjava3.core.Flowable;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class StoryFactoryController {

    private final StoryFactoryAgent storyFactoryAgent;

    public StoryFactoryController(StoryFactoryAgent storyFactoryAgent) {
        this.storyFactoryAgent = storyFactoryAgent;
    }

    @PostMapping(value = "/generate-story", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<String> generateStory(@RequestBody StoryRequest request) {
        RunConfig runConfig = RunConfig.builder().build();
        InMemoryRunner runner = new InMemoryRunner(storyFactoryAgent.getAgent());

        // Create a session for the conversation
        Session session = runner.sessionService()
                .createSession(runner.appName(), "factory-user")
                .blockingGet(); // Session creation is a quick, one-time setup

        Content userMsg = Content.fromParts(Part.fromText(request.getTopic()));

        // Run the agent and get the reactive stream of events
        Flowable<Event> events = runner.runAsync(session.userId(), session.id(), userMsg, runConfig);

        // Bridge the RxJava Flowable to a Project Reactor Mono for WebFlux
        // This is a fully non-blocking chain
        return Mono.fromCompletionStage(
            events
                .filter(Event::finalResponse) // We only care about the final output
                .map(Event::stringifyContent) // Extract the string content
                .firstElement() // Get the first (and only) final response
                .toCompletionStage() // Convert RxJava's Maybe to a standard CompletionStage
        ).defaultIfEmpty("{\"error\": \"No final response from agent.\"}");
    }
}