package com.hyper_engineer.superstoryfactory.controllers;

import com.google.adk.agents.RunConfig;
import com.google.adk.events.Event;
import com.google.adk.runner.InMemoryRunner;
import com.google.adk.sessions.Session;
import com.google.genai.types.Content;
import com.google.genai.types.Part;
import com.google.gson.Gson;
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

        Flowable<Event> routerEvents = Flowable.defer(() -> {
            Session session = routerRunner.sessionService()
                    .createSession(routerRunner.appName(), "unified-user-" + System.currentTimeMillis())
                    .blockingGet();
            return routerRunner.runAsync(session.userId(), session.id(), userMsg, runConfig);
        });

        Flux<Event> routerFlux = Flux.from(routerEvents)
                .doOnNext(e -> log.info("[ROUTER_EVENT] {}", e))
                .cache();

        Mono<Event> routerDecision = routerFlux
                .filter(e -> "master-router-agent".equals(e.author()) && e.finalResponse())
                .last()
                .doOnSuccess(e -> log.info("Router decision event: {}", e));

        return routerDecision.flatMapMany(decisionEvent -> {
            String decision = decisionEvent.stringifyContent();
            if ("DEFAULT".equalsIgnoreCase(decision.trim())) {
                log.info("Routing to DEFAULT (Streaming Chat)");
                return executeStreamingAgent(userMsg, runConfig)
                        .filter(this::isLlmChunkEvent) // Filter for streamable chunks
                        .map(Event::stringifyContent); // Convert to String
            } else {
                log.info("Routing to SPECIALIZED TOOL output");
                return routerFlux
                        .filter(e -> !e.functionResponses().isEmpty())
                        .map(this::extractContentFromResult);
            }
        }).onErrorResume(e -> {
            log.error("An error occurred in the chat stream: {}", e.getMessage(), e);
            String friendlyMessage = "Sorry, I encountered an error. Please try again.";
            if (e.getMessage() != null && e.getMessage().contains("429")) {
                friendlyMessage = "I'm experiencing high traffic right now. Please wait a moment before sending another request.";
            }
            return Flux.just(friendlyMessage);
        });
    }

    private Flux<Event> executeStreamingAgent(Content userMsg, RunConfig runConfig) {
        Session chatSession = streamingChatRunner.sessionService()
                .createSession(streamingChatRunner.appName(), "streaming-delegate-" + System.currentTimeMillis())
                .blockingGet();
        return Flux.from(streamingChatRunner.runAsync(chatSession.userId(), chatSession.id(), userMsg, runConfig));
    }

    private String extractContentFromResult(Event event) {
        if (event.functionResponses().isEmpty()) {
            return event.stringifyContent();
        }

        try {
            // Correctly handle the generic Optional<?> returned by the method
            Optional<?> responseObjectOpt = event.functionResponses().get(0).response();
            if (responseObjectOpt.isPresent()) {
                Object responseData = responseObjectOpt.get();
                if (responseData instanceof Map) {
                    // We can safely cast here after the check
                    @SuppressWarnings("unchecked")
                    Map<String, Object> responseMap = (Map<String, Object>) responseData;
                    if (responseMap.containsKey("result")) {
                        return responseMap.get("result").toString();
                    }
                }
                return gson.toJson(responseData);
            }
        } catch (Exception e) {
            log.error("Error parsing function response content: {}", e.getMessage());
        }

        return event.stringifyContent();
    }

    private boolean isLlmChunkEvent(Event event) {
        boolean isModelAuthor = "model".equals(event.author());
        boolean isPartial = event.partial().orElse(false);
        boolean hasNoFunctionCalls = event.functionCalls().isEmpty();
        return isModelAuthor && isPartial && hasNoFunctionCalls;
    }
}