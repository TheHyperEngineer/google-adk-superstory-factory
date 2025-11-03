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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@CrossOrigin(origins = "http://localhost:5173")
public class StreamingChatController {

    private static final Logger log = LoggerFactory.getLogger(StreamingChatController.class);
    private final InMemoryRunner streamingChatRunner;

    public StreamingChatController(@Qualifier("streamingChatRunner") InMemoryRunner streamingChatRunner) {
        this.streamingChatRunner = streamingChatRunner;
    }

    @GetMapping(value = "/stream-chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamChat(@RequestParam String userQuestion) {
        log.info("Received /stream-chat request for topic: {}", userQuestion);
        StoryRequest request = new StoryRequest();
        request.setTopic(userQuestion);
        RunConfig runConfig = RunConfig.builder().build();

        Session session = streamingChatRunner.sessionService()
                .createSession(streamingChatRunner.appName(), "streaming-user")
                .blockingGet();

        Content userMsg = Content.fromParts(Part.fromText(request.getTopic()));
        Flowable<Event> events = streamingChatRunner.runAsync(session.userId(), session.id(), userMsg, runConfig);

        return Flux.from(events)
                .doOnNext(event -> log.info("[Event Log] Author: {}, Partial: {}, Final: {}, Content: {}",
                        event.author(), event.partial().orElse(false), event.finalResponse(), event.stringifyContent()))
                .doOnError(error -> log.error("Error in streaming event stream", error))
                .filter(this::isLlmChunkOrFinalResponseEvent) // Using the new, more robust filter
                .map(Event::stringifyContent)
                .doOnComplete(() -> log.info("Streaming chat completed successfully."));
    }

    /**
     * A robust filter that allows either a partial streaming chunk OR the final,
     * complete response from the model to pass through. This handles both true
     * streaming and single-response scenarios.
     *
     * @param event The event to inspect.
     * @return true if the event is a valid text response from the model.
     */
    private boolean isLlmChunkOrFinalResponseEvent(Event event) {
        // Must be from the model itself, not the user or a tool.
        // The agent name is used as the author for the final response.
        boolean isFromModelOrAgent = "model".equals(event.author()) || "markdown-streaming-agent".equals(event.author());

        // Must not be a function call.
        boolean hasNoFunctionCalls = event.functionCalls().isEmpty();

        // It's either a partial chunk OR it's the final response from the agent.
        boolean isPartial = event.partial().orElse(false);
        boolean isFinalResponse = event.finalResponse();

        return isFromModelOrAgent && hasNoFunctionCalls && (isPartial || isFinalResponse);
    }
}