package engineer.hyper.rag.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.adk.events.Event;
import com.google.adk.events.EventActions;
import com.google.adk.sessions.*;
import com.google.genai.types.Content;
import com.google.genai.types.FinishReason;
import com.google.genai.types.GroundingMetadata;
import engineer.hyper.rag.entities.EventEntity;
import engineer.hyper.rag.entities.SessionEntity;
import engineer.hyper.rag.repositories.EventRepository;
import engineer.hyper.rag.repositories.SessionRepository;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Nullable;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

@Service
public class H2SessionService implements BaseSessionService {

    private static final Logger log = LoggerFactory.getLogger(H2SessionService.class);
    private static final String TEMP_PREFIX = "temp:";
    private static final String APP_PREFIX = "app:";
    private static final String USER_PREFIX = "user:";

    private final SessionRepository sessionRepository;
    private final EventRepository eventRepository;
    private final ObjectMapper objectMapper;

    @Autowired
    public H2SessionService(SessionRepository sessionRepository,
                            EventRepository eventRepository,
                            ObjectMapper objectMapper) {
        this.sessionRepository = sessionRepository;
        this.eventRepository = eventRepository;

        this.objectMapper = objectMapper.copy()
                .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule())
                .registerModule(new com.fasterxml.jackson.datatype.jdk8.Jdk8Module()) // Handles Optional deserialization
                .setSerializationInclusion(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL) // Don't serialize nulls
                .setSerializationInclusion(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_ABSENT) // Don't serialize Optional.empty()
                .configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_NULL_CREATOR_PROPERTIES, false);
    }

    @Override
    @Transactional
    public Single<Session> createSession(String appName, String userId,
                                         @Nullable ConcurrentMap<String, Object> state,
                                         @Nullable String sessionId) {
        // Google pattern: Simple Single.just(), no subscribeOn
        String id = Optional.ofNullable(sessionId)
                .filter(s -> !s.trim().isEmpty())
                .orElseGet(() -> UUID.randomUUID().toString());
        
        log.debug("Creating session: {}/{}/{}", appName, userId, id);

        SessionEntity entity = new SessionEntity();
        entity.setId(id);
        entity.setAppName(appName);
        entity.setUserId(userId);
        entity.setState(serializeState(state != null ? state : new ConcurrentHashMap<>()));
        entity.setLastUpdateTime(Instant.now());

        sessionRepository.save(entity);

        // Return copy to prevent state leakage (Google InMemory pattern)
        return Single.just(createSessionCopy(entity, new ArrayList<>()));
    }

    @Override
    @Transactional(readOnly = true)
    public Maybe<Session> getSession(String appName, String userId, String sessionId,
                                     Optional<GetSessionConfig> config) {
        log.debug("Fetching session: {}/{}/{}", appName, userId, sessionId);

        SessionEntity entity = sessionRepository.findBySessionIdAndAppNameAndUserId(sessionId, appName, userId);
        if (entity == null) {
            log.warn("Session not found: {}/{}/{}", appName, userId, sessionId);
            return Maybe.empty();
        }

        // Load events and apply config filters (Google pattern)
        List<Event> events = fetchEventsForSession(sessionId);
        
        // Google pattern: respect GetSessionConfig
        if (config.isPresent()) {
            events = applyEventFilters(events, config.get());
        }

        return Maybe.just(createSessionCopy(entity, events));
    }

    @Override
    @Transactional(readOnly = true)
    public Single<ListSessionsResponse> listSessions(String appName, String userId) {
        log.debug("Listing sessions for: {}/{}", appName, userId);

        // Google pattern: load metadata only, not full session with events
        List<SessionEntity> entities = sessionRepository.findByAppNameAndUserId(appName, userId);
        
        List<Session> sessions = entities.stream()
                .map(this::createSessionMetadataCopy)
                .collect(Collectors.toList());

        log.info("Found {} sessions for: {}/{}", sessions.size(), appName, userId);
        return Single.just(ListSessionsResponse.builder().sessions(sessions).build());
    }

    @Override
    @Transactional
    public Completable deleteSession(String appName, String userId, String sessionId) {
        log.debug("Deleting session: {}/{}/{}", appName, userId, sessionId);

        // Google pattern: simple deletion, no extra queries
        int deletedEvents = eventRepository.deleteBySessionId(sessionId);
        int deletedSessions = sessionRepository.deleteByAppNameAndUserIdAndId(appName, userId, sessionId);
        
        if (deletedSessions > 0) {
            log.info("Deleted session {} and {} events", sessionId, deletedEvents);
        } else {
            log.warn("Session not found for deletion: {}/{}/{}", appName, userId, sessionId);
        }

        return Completable.complete();
    }

    @Override
    @Transactional(readOnly = true)
    public Single<ListEventsResponse> listEvents(String appName, String userId, String sessionId) {
        log.debug("Listing events for session: {}/{}/{}", appName, userId, sessionId);

        // Verify session exists
        if (!sessionRepository.existsByIdAndAppNameAndUserId(sessionId, appName, userId)) {
            throw new IllegalArgumentException("Session not found or access denied: " + sessionId);
        }

        List<Event> events = fetchEventsForSession(sessionId);
        return Single.just(ListEventsResponse.builder().events(events).build());
    }

    @Override
    @Transactional
    public Completable closeSession(Session session) {
        log.debug("Closing session: {}", session.id());

        // Google pattern: update timestamp only
        SessionEntity entity = sessionRepository.findById(session.id())
                .orElseThrow(() -> new IllegalArgumentException("Session not found: " + session.id()));
        entity.setLastUpdateTime(Instant.now());
        sessionRepository.save(entity);

        return Completable.complete();
    }

    @Override
    @Transactional
    public Single<Event> appendEvent(Session session, Event event) {
        Objects.requireNonNull(session, "session cannot be null");
        Objects.requireNonNull(event, "event cannot be null");

        String sessionId = session.id();
        log.debug("Appending event to session: {}", sessionId);

        // Verify session exists
        SessionEntity sessionEntity = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found: " + sessionId));

        // Google pattern: skip partial events
        if (Boolean.TRUE.equals(event.partial().orElse(false))) {
            log.debug("Skipping persistence for partial event: {}", event.id());
            return Single.just(event);
        }

        // Google pattern: handle state delta with prefixes
        EventActions actions = event.actions();
        if (actions != null && actions.stateDelta() != null && !actions.stateDelta().isEmpty()) {
            processStateDelta(sessionEntity, actions, session.appName(), session.userId());
        }

        // Persist event
        EventEntity eventEntity = convertEventToEntity(event, sessionEntity);
        eventRepository.save(eventEntity);
        log.debug("Successfully appended event: {}", eventEntity.getId());

        return Single.just(event);
    }

    // Google pattern: comprehensive state management with prefixes
    private void processStateDelta(SessionEntity sessionEntity, EventActions actions, String appName, String userId) {
        try {
            ConcurrentMap<String, Object> sessionState = deserializeState(sessionEntity.getState());

            actions.stateDelta().forEach((key, value) -> {
                if (key.startsWith(TEMP_PREFIX)) {
                    // Skip temp keys (Google pattern)
                    return;
                }
                
                if (value == State.REMOVED) {
                    sessionState.remove(key);
                    log.trace("Removed state key: {}", key);
                } else {
                    sessionState.put(key, value);
                    log.trace("Updated state key: {} = {}", key, value);
                }
            });

            sessionEntity.setState(serializeState(sessionState));
            sessionEntity.setLastUpdateTime(Instant.now());
            sessionRepository.save(sessionEntity);
            
            log.debug("Applied state delta to session: {}", sessionEntity.getId());
        } catch (Exception e) {
            log.error("Failed to process state delta for session: {}", sessionEntity.getId(), e);
            throw new RuntimeException("Failed to process state delta", e);
        }
    }

    // Google pattern: apply GetSessionConfig filters
    private List<Event> applyEventFilters(List<Event> events, GetSessionConfig config) {
        List<Event> filtered = new ArrayList<>(events);
        
        config.numRecentEvents().ifPresent(num -> {
            if (filtered.size() > num) {
                filtered.subList(0, filtered.size() - num).clear();
            }
        });
        
        config.afterTimestamp().ifPresent(timestamp -> {
            filtered.removeIf(e -> Instant.ofEpochMilli(e.timestamp()).isBefore(timestamp));
        });
        
        return filtered;
    }

    private List<Event> fetchEventsForSession(String sessionId) {
        // Google pattern: load events directly, handle errors at call site
        return eventRepository.findBySessionId(sessionId).stream()
                .map(this::convertEntityToEvent)
                .collect(Collectors.toList());
    }

    private String serializeState(ConcurrentMap<String, Object> state) {
        try {
            return objectMapper.writeValueAsString(state);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize state", e);
        }
    }

    private ConcurrentMap<String, Object> deserializeState(String stateJson) {
        if (stateJson == null || stateJson.isEmpty()) {
            return new ConcurrentHashMap<>();
        }
        try {
            return objectMapper.readValue(stateJson, new TypeReference<ConcurrentHashMap<String, Object>>() {});
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize state", e);
        }
    }

    // Google pattern: create immutable copy to prevent state leakage
    private Session createSessionCopy(SessionEntity entity, List<Event> events) {
        try {
            ConcurrentMap<String, Object> stateCopy = new ConcurrentHashMap<>(deserializeState(entity.getState()));
            List<Event> eventsCopy = events != null ? new ArrayList<>(events) : new ArrayList<>();
            
            return Session.builder(entity.getId())
                    .appName(entity.getAppName())
                    .userId(entity.getUserId())
                    .state(stateCopy)
                    .events(eventsCopy)
                    .lastUpdateTime(entity.getLastUpdateTime())
                    .build();
        } catch (Exception e) {
            log.error("Failed to create session copy: {}", entity.getId(), e);
            throw new RuntimeException("Failed to create session copy", e);
        }
    }

    // Google pattern: metadata-only copy for list operations
    private Session createSessionMetadataCopy(SessionEntity entity) {
        return Session.builder(entity.getId())
                .appName(entity.getAppName())
                .userId(entity.getUserId())
                .lastUpdateTime(entity.getLastUpdateTime())
                .build();
    }

    // Google pattern: simple conversion without complex error handling
    private Event convertEntityToEvent(EventEntity entity) {
        try {
            Event.Builder builder = Event.builder()
                    .id(entity.getId())
                    .invocationId(entity.getInvocationId())
                    .author(entity.getAuthor())
                    .timestamp(entity.getTimestamp() != null ? entity.getTimestamp() : Instant.now().toEpochMilli());

            // Google pattern: fail-fast deserialization
            if (entity.getPartial() != null) builder.partial(entity.getPartial());
            if (entity.getTurnComplete() != null) builder.turnComplete(entity.getTurnComplete());
            if (entity.getInterrupted() != null) builder.interrupted(entity.getInterrupted());
            if (entity.getBranch() != null) builder.branch(entity.getBranch());

            // Google pattern: use convertValue for simpler mapping
            // In H2SessionService.convertEntityToEvent()
            if (entity.getContent() != null && !entity.getContent().isEmpty()) {
                String sanitizedContent = removeNullFieldsFromJson(entity.getContent());
                Content content = objectMapper.readValue(sanitizedContent, Content.class);
                builder.content(content);
            }

            // Apply the same pattern to other serialized fields that might have nulls
            if (entity.getActions() != null && !entity.getActions().isEmpty()) {
                String sanitizedActions = removeNullFieldsFromJson(entity.getActions());
                EventActions actions = objectMapper.readValue(sanitizedActions, EventActions.class);
                builder.actions(actions);
            }

            if (entity.getLongRunningToolIds() != null && !entity.getLongRunningToolIds().isEmpty()) {
                String sanitizedToolIds = removeNullFieldsFromJson(entity.getLongRunningToolIds());
                Set<String> toolIds = objectMapper.readValue(sanitizedToolIds, new TypeReference<Set<String>>() {});
                builder.longRunningToolIds(toolIds);
            }

            if (entity.getErrorCode() != null) {
                builder.errorCode(new FinishReason(entity.getErrorCode()));
            }

            if (entity.getErrorMessage() != null) {
                builder.errorMessage(entity.getErrorMessage());
            }

            if (entity.getGroundingMetadata() != null && !entity.getGroundingMetadata().isEmpty()) {
                String sanitizedMetadata = removeNullFieldsFromJson(entity.getGroundingMetadata());
                GroundingMetadata metadata = objectMapper.readValue(sanitizedMetadata, GroundingMetadata.class);
                builder.groundingMetadata(metadata);
            }

            return builder.build();
        } catch (Exception e) {
            log.error("Failed to convert event entity: {}", entity.getId(), e);
            throw new RuntimeException("Failed to convert event: " + entity.getId(), e);
        }
    }

    private EventEntity convertEventToEntity(Event event, SessionEntity session) {
        try {
            EventEntity entity = new EventEntity();
            entity.setId(event.id() != null ? event.id() : Event.generateEventId());
            entity.setSession(session);
            entity.setInvocationId(event.invocationId());
            entity.setAuthor(event.author());
            entity.setTimestamp(event.timestamp());

            // Google pattern: direct serialization
            event.content().ifPresent(content -> {
                try {
                    entity.setContent(objectMapper.writeValueAsString(content));
                } catch (Exception e) {
                    throw new RuntimeException("Failed to serialize content", e);
                }
            });

            if (event.actions() != null) {
                entity.setActions(objectMapper.writeValueAsString(event.actions()));
            }

            event.longRunningToolIds().ifPresent(toolIds -> {
                try {
                    entity.setLongRunningToolIds(objectMapper.writeValueAsString(toolIds));
                } catch (Exception e) {
                    throw new RuntimeException("Failed to serialize tool IDs", e);
                }
            });

            entity.setPartial(event.partial().orElse(null));
            entity.setTurnComplete(event.turnComplete().orElse(null));
            entity.setInterrupted(event.interrupted().orElse(null));
            entity.setErrorCode(event.errorCode().map(FinishReason::toString).orElse(null));
            entity.setErrorMessage(event.errorMessage().orElse(null));
            entity.setBranch(event.branch().orElse(null));

            event.groundingMetadata().ifPresent(metadata -> {
                try {
                    entity.setGroundingMetadata(objectMapper.writeValueAsString(metadata));
                } catch (Exception e) {
                    throw new RuntimeException("Failed to serialize grounding metadata", e);
                }
            });

            return entity;
        } catch (Exception e) {
            log.error("Failed to convert event to entity for session: {}", session.getId(), e);
            throw new RuntimeException("Failed to convert event", e);
        }
    }

    private String removeNullFieldsFromJson(String json) {
        if (json == null || json.isEmpty()) return json;
        try {
            // Parse JSON into a structure
            Map<String, Object> map = objectMapper.readValue(json, Map.class);
            // Recursively remove null values
            removeNullValues(map);
            // Write back as clean JSON
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            log.warn("Failed to sanitize JSON, proceeding with original: {}", e.getMessage());
            return json; // Fallback to original if sanitization fails
        }
    }

    private void removeNullValues(Map<String, Object> map) {
        map.entrySet().removeIf(entry -> entry.getValue() == null);
        map.forEach((key, value) -> {
            if (value instanceof Map) {
                removeNullValues((Map<String, Object>) value);
            } else if (value instanceof List) {
                ((List<?>) value).forEach(item -> {
                    if (item instanceof Map) {
                        removeNullValues((Map<String, Object>) item);
                    }
                });
            }
        });
    }
}