package engineer.hyper.rag.controllers;

import com.google.adk.events.Event;
import com.google.adk.sessions.GetSessionConfig;
import com.google.adk.sessions.Session;
import engineer.hyper.rag.dtos.EventListDto;
import engineer.hyper.rag.dtos.SessionDto;
import engineer.hyper.rag.dtos.SessionListDto;
import engineer.hyper.rag.services.H2SessionService;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/sessions")
public class SessionController {

    @Autowired
    private H2SessionService sessionService;

    @PostMapping
    public Single<ResponseEntity<SessionDto>> createSession(
            @RequestParam @NotBlank String appName,
            @RequestParam @NotBlank String userId,
            @RequestParam(required = false) String sessionId) {

        return sessionService.createSession(appName, userId, null, sessionId)
                .map(session -> ResponseEntity.status(HttpStatus.CREATED)
                        .body(new SessionDto(session)));
    }

    @GetMapping("/{sessionId}")
    public Maybe<ResponseEntity<SessionDto>> getSession(
            @PathVariable @NotBlank String sessionId,
            @RequestParam @NotBlank String appName,
            @RequestParam @NotBlank String userId,
            @RequestParam(required = false) Integer numRecentEvents,
            @RequestParam(required = false) Long afterTimestamp) {

        GetSessionConfig.Builder configBuilder = GetSessionConfig.builder();
        Optional.ofNullable(numRecentEvents).ifPresent(configBuilder::numRecentEvents);
        Optional.ofNullable(afterTimestamp).map(java.time.Instant::ofEpochSecond)
                .ifPresent(configBuilder::afterTimestamp);

        return sessionService.getSession(appName, userId, sessionId,
                        Optional.of(configBuilder.build()))
                .map(session -> ResponseEntity.ok(new SessionDto(session)))
                .switchIfEmpty(Maybe.just(ResponseEntity.notFound().build()));
    }

    @GetMapping
    public Single<SessionListDto> listSessions(
            @RequestParam @NotBlank String appName,
            @RequestParam @NotBlank String userId) {

        return sessionService.listSessions(appName, userId)
                .map(response -> new SessionListDto(response.sessions()));
    }

    @DeleteMapping("/{sessionId}")
    public Completable deleteSession(
            @PathVariable @NotBlank String sessionId,
            @RequestParam @NotBlank String appName,
            @RequestParam @NotBlank String userId) {

        return sessionService.deleteSession(appName, userId, sessionId);
    }

    @PostMapping("/{sessionId}/events")
    public Single<Event> appendEvent(
            @PathVariable @NotBlank String sessionId,
            @RequestParam @NotBlank String appName,
            @RequestParam @NotBlank String userId,
            @RequestBody @Valid Event event) {

        Session session = Session.builder(sessionId)
                .appName(appName)
                .userId(userId)
                .state(new ConcurrentHashMap<>())
                .build();

        return sessionService.appendEvent(session, event);
    }

    @GetMapping("/{sessionId}/events")
    public Single<EventListDto> listEvents(
            @PathVariable @NotBlank String sessionId,
            @RequestParam @NotBlank String appName,
            @RequestParam @NotBlank String userId) {

        return sessionService.listEvents(appName, userId, sessionId)
                .map(response -> new EventListDto(response.events()));
    }

    @PostMapping("/{sessionId}/close")
    public Completable closeSession(
            @PathVariable @NotBlank String sessionId,
            @RequestParam @NotBlank String appName,
            @RequestParam @NotBlank String userId) {

        Session session = Session.builder(sessionId)
                .appName(appName)
                .userId(userId)
                .build();

        return sessionService.closeSession(session);
    }
}
