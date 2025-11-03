package com.hyper_engineer.superstoryfactory.controllers;

import com.google.gson.Gson;
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
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:5173")
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);
    private final InMemoryRunner routerRunner;
    private final InMemoryRunner streamingChatRunner;
    private static final Gson gson = new Gson();

    public ChatController(
            @Qualifier("routerRunner") InMemoryRunner routerRunner,
            @Qualifier("streamingChatRunner") InMemoryRunner streamingChatRunner) {
        this.routerRunner = routerRunner;
        this.streamingChatRunner = streamingChatRunner;
    }

    @GetMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> handleChat(@RequestParam String prompt) {
        log.info("Received unified chat request: {}", prompt);

        RunConfig runConfig = RunConfig.builder().build();
        Content userMsg = Content.fromParts(Part.fromText(prompt));

        // Use a Mono to represent the single, decisive final event from the router.
        // This entire block runs asynchronously but completes with one item.
        Mono<Event> routerDecisionMono = Mono.fromCallable(() -> {
            Session session = routerRunner.sessionService()
                    .createSession(routerRunner.appName(), "unified-user-" + System.currentTimeMillis())
                    .blockingGet();
            // Execute the router and get the *last* event, which contains the final decision.
            return routerRunner.runAsync(session.userId(), session.id(), userMsg, runConfig).blockingLast();
        }).doOnSuccess(event -> log.info("Router decision event: {}", event));

        // Use the result of the Mono to switch to the correct final stream (Flux).
        return routerDecisionMono.flatMapMany(decisionEvent -> {
            String decisionText = decisionEvent.stringifyContent();

            // CASE 1: The router decided to use the default chat agent.
            if ("DEFAULT".equalsIgnoreCase(decisionText.trim())) {
                log.info("Routing to DEFAULT (Streaming Chat).");
                // Execute the streaming agent with the correct streaming configuration.
                return executeStreamingAgent(userMsg, runConfig)
                        .filter(this::isStreamableChatEvent)
                        .map(Event::stringifyContent);
            }
            // CASE 2: The router used a specialized tool (Joker or Story Factory).
            else {
                log.info("Routing to SPECIALIZED TOOL output.");
                // The final, complete answer is the content of the router's last event.
                // Return a stream containing just this single, complete answer.
                return Flux.just(decisionText);
            }
        }).onErrorResume(e -> {
            log.error("An error occurred in the chat stream: {}", e.getMessage(), e);
            String friendlyMessage = "Sorry, an error occurred. Please try again.";
            if (e.getMessage() != null && e.getMessage().contains("429")) {
                friendlyMessage = "Too many requests. Please wait a moment before trying again.";
            }
            return Flux.just(friendlyMessage);
        });
    }

    private Flux<Event> executeStreamingAgent(Content userMsg, RunConfig runConfig) {
        Session chatSession = streamingChatRunner.sessionService()
                .createSession(streamingChatRunner.appName(), "streaming-delegate-" + System.currentTimeMillis())
                .blockingGet();

        // THIS IS THE CORRECT WAY TO ENABLE STREAMING based on the provided source code.
        RunConfig streamingRunConfig = RunConfig.builder(runConfig)
                .setStreamingMode(RunConfig.StreamingMode.SSE)
                .build();

        return Flux.from(streamingChatRunner.runAsync(chatSession.userId(), chatSession.id(), userMsg, streamingRunConfig));
    }

    private boolean isStreamableChatEvent(Event event) {
        // This filter is for the streaming agent ONLY.
        // It must accept partial chunks from the model.
        boolean isModelChunk = "model".equals(event.author()) && event.partial().orElse(false);
        // It must also accept the final consolidated event from the agent itself.
        boolean isFinalAgentResponse = "markdown-streaming-agent".equals(event.author()) && event.finalResponse();

        return (isModelChunk || isFinalAgentResponse) && event.functionCalls().isEmpty();
    }

    // This helper is no longer needed with the simplified logic but is kept for reference.
    private String extractContentFromResult(Event event) {
        if (event.functionResponses().isEmpty()) {
            return event.stringifyContent();
        }
        try {
            Optional<?> responseObjectOpt = event.functionResponses().get(0).response();
            if (responseObjectOpt.isPresent()) {
                Object responseData = responseObjectOpt.get();
                if (responseData instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> responseMap = (Map<String, Object>) responseData;
                    if (responseMap.containsKey("result")) {
                        return responseMap.get("result").toString();
                    }
                }
                return new Gson().toJson(responseData);
            }
        } catch (Exception e) {
            log.error("Error parsing function response content: {}", e.getMessage());
        }
        return event.stringifyContent();
    }
}