package com.hyper_engineer.superstoryfactory.controllers;

import com.google.adk.agents.RunConfig;
import com.google.adk.events.Event;
import com.google.adk.runner.InMemoryRunner;
import com.google.adk.sessions.Session;
import com.google.genai.types.Content;
import com.google.genai.types.Part;
import com.hyper_engineer.superstoryfactory.controllers.dto.StoryRequest;
import io.reactivex.rxjava3.core.Flowable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@CrossOrigin(origins = "http://localhost:5173") // Allow requests from your React app's origin
public class StoryFactoryController {

    private static final Logger log = LoggerFactory.getLogger(StoryFactoryController.class);
    private final InMemoryRunner storyFactoryRunner;

    public StoryFactoryController(@Qualifier("storyFactoryRunner") InMemoryRunner storyFactoryRunner) {
        this.storyFactoryRunner = storyFactoryRunner;
    }

    @PostMapping(value = "/generate-story", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<String> generateStory(@RequestBody StoryRequest request) {
        log.info("Received /generate-story request for topic: {}", request.getTopic());
        RunConfig runConfig = RunConfig.builder().build();

        Session session = storyFactoryRunner.sessionService()
                .createSession(storyFactoryRunner.appName(), "factory-user")
                .blockingGet();

        Content userMsg = Content.fromParts(Part.fromText(request.getTopic()));
        Flowable<Event> events = storyFactoryRunner.runAsync(session.userId(), session.id(), userMsg, runConfig);

        return Mono.fromCompletionStage(
                        events
                                .doOnNext(event -> log.info("[Event Log] Author: {}, Final: {}, Content: {}",
                                        event.author(), event.finalResponse(), event.stringifyContent()))
                                .doOnError(error -> log.error("Error in event stream", error))
                                // More specific filter: We want the final response from the COMPILER.
                                .filter(e -> "report-compiler".equals(e.author()) && e.finalResponse())
                                .map(Event::stringifyContent)
                                .lastElement()
                                .toCompletionStage()
                ).doOnSuccess(response -> log.info("Successfully generated story response."))
                .defaultIfEmpty("{\"error\": \"No final response from compiler agent.\"}");
    }
}