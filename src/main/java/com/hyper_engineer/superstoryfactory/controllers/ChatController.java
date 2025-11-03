package com.hyper_engineer.superstoryfactory.controllers;

import com.google.adk.agents.RunConfig;
import com.google.adk.events.Event;
import com.google.adk.runner.InMemoryRunner;
import com.google.adk.sessions.Session;
import com.google.genai.types.Content;
import com.google.genai.types.Part;
import io.reactivex.rxjava3.core.Flowable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:5173")
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);
    private final InMemoryRunner routerRunner;

    public ChatController(@Qualifier("routerRunner") InMemoryRunner routerRunner) {
        this.routerRunner = routerRunner;
    }

    @GetMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> handleChat(@RequestParam String prompt) {
        log.info("Received unified chat request: {}", prompt);

        // Enable streaming for the entire execution.
        RunConfig streamingConfig = RunConfig.builder()
                .setStreamingMode(RunConfig.StreamingMode.SSE)
                .build();

        Content userMsg = Content.fromParts(Part.fromText(prompt));

        // Create a deferred Flowable to ensure a new session for each request.
        Flowable<Event> allEvents = Flowable.defer(() -> {
            Session session = routerRunner.sessionService()
                    .createSession(routerRunner.appName(), "unified-user-" + System.currentTimeMillis())
                    .blockingGet();
            return routerRunner.runAsync(session.userId(), session.id(), userMsg, streamingConfig);
        });

        return Flux.from(allEvents)
                .doOnNext(event -> log.debug("[EVENT]: {}", event.toString()))
                .map(this::extractContentForClient)
                .filter(content -> content != null && !content.isEmpty())
                .onErrorResume(e -> {
                    log.error("An error occurred in the chat stream: {}", e.getMessage(), e);
                    String friendlyMessage = "Sorry, an error occurred. Please try again.";
                    if (e.getMessage() != null && e.getMessage().contains("429")) {
                        friendlyMessage = "Too many requests. Please wait a moment.";
                    }
                    return Flux.just(friendlyMessage);
                });
    }

    private String extractContentForClient(Event event) {
        // Case 1: A streaming text chunk from the model (the "typewriter" effect).
        if ("model".equals(event.author()) && event.partial().orElse(false)) {
            return event.stringifyContent();
        }

        // Case 2: The final, complete output from a specialized agent (tool).
        if (!event.functionResponses().isEmpty()) {
            try {
                Optional<?> responseObjectOpt = event.functionResponses().get(0).response();
                if (responseObjectOpt.isPresent() && responseObjectOpt.get() instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> responseMap = (Map<String, Object>) responseObjectOpt.get();
                    return (String) responseMap.getOrDefault("result", "");
                }
            } catch (Exception e) {
                log.error("Could not parse function response content", e);
                return "";
            }
        }
        return "";
    }
}