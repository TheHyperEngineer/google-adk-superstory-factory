package engineer.hyper.rag.services;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.adk.events.Event;
import com.google.adk.memory.BaseMemoryService;
import com.google.adk.memory.MemoryEntry;
import com.google.adk.memory.SearchMemoryResponse;
import com.google.adk.sessions.ListEventsResponse;
import com.google.adk.sessions.Session;
import com.google.genai.types.Content;
import engineer.hyper.rag.entities.MemoryEntryEntity;
import engineer.hyper.rag.repositories.MemoryEntryRepository;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class H2MemoryService implements BaseMemoryService {
    
    private static final Logger log = LoggerFactory.getLogger(H2MemoryService.class);
    
    private final MemoryEntryRepository repository;
    private final ObjectMapper objectMapper;
    
    @Autowired
    public H2MemoryService(MemoryEntryRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
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
    public Completable addSessionToMemory(Session session) {
        return Completable.fromRunnable(() -> {
            log.debug("Adding session to memory: {}", session.id());
            
            String appName = session.appName();
            String userId = session.userId();
            String sessionId = session.id();
            
            List<MemoryEntryEntity> entries = new ArrayList<>();
            
            // Process each event in the session to extract memory entries
            for (Event event : session.events()) {
                // Skip events without content
                Optional<Content> contentOpt = event.content();
                if (!contentOpt.isPresent()) {
                    continue;
                }
                
                Content content = contentOpt.get();
                
                // Skip content without parts or with empty parts
                if (!content.parts().isPresent() || content.parts().get().isEmpty()) {
                    continue;
                }
                
                // Extract searchable text from content
                String searchText = Optional.ofNullable(content.text()).orElse("");
                if (searchText.isEmpty()) {
                    // Still store even if no text, but limit searchability
                    log.trace("No searchable text found for event: {}", event.id());
                }
                
                // Truncate search text to fit column length
                if (searchText.length() > 4000) {
                    searchText = searchText.substring(0, 3997) + "...";
                }
                
                // Create memory entry entity
                MemoryEntryEntity entity = new MemoryEntryEntity();
                entity.setId(UUID.randomUUID().toString());
                entity.setAppName(appName);
                entity.setUserId(userId);
                entity.setSessionId(sessionId);
                entity.setAuthor(event.author());
                entity.setTimestamp(Instant.ofEpochMilli(event.timestamp()).toString());
                entity.setSearchText(searchText);
                
                // Serialize content to JSON
                try {
                    entity.setContent(objectMapper.writeValueAsString(content));
                } catch (Exception e) {
                    log.error("Failed to serialize content for event: {}", event.id(), e);
                    continue; // Skip this event but continue processing others
                }
                
                entries.add(entity);
            }
            
            // Save all entries in batch
            if (!entries.isEmpty()) {
                repository.saveAll(entries);
                log.info("Successfully added {} memory entries for session: {}", entries.size(), sessionId);
            } else {
                log.warn("No valid content events found in session: {}", sessionId);
            }
        }).subscribeOn(Schedulers.io());
    }
    
    @Override
    @Transactional(readOnly = true)
    public Single<SearchMemoryResponse> searchMemory(String appName, String userId, String query) {
        log.debug("Searching memory for app: {}, user: {}, query: '{}'", appName, userId, query);

        if (query == null || query.trim().isEmpty()) {
            return Single.just(SearchMemoryResponse.builder().setMemories(new ArrayList<>()).build());
        }

        // Perform case-insensitive search
        List<MemoryEntryEntity> entities = repository.searchByAppNameAndUserIdAndQuery(appName, userId, query.trim());

        log.debug("Found {} memory entries matching query", entities.size());

        // Convert entities to MemoryEntry objects
        List<MemoryEntry> memories = entities.stream()
                .map(this::toMemoryEntry)
                .collect(Collectors.toList());

        SearchMemoryResponse searchMemoryResponse = SearchMemoryResponse.builder()
                .setMemories(memories)
                .build();
        log.info("Search completed with {} results for app: {}, user: {}", memories.size(), appName, userId);
        return Single.just(searchMemoryResponse);
    }
    
    /**
     * Converts a MemoryEntryEntity to a MemoryEntry domain object
     */
    private MemoryEntry toMemoryEntry(MemoryEntryEntity entity) {
        try {
            // Deserialize JSON content back to Content object
            Content content = objectMapper.readValue(entity.getContent(), Content.class);
            
            return MemoryEntry.builder()
                .content(content)
                .author(entity.getAuthor())
                .timestamp(entity.getTimestamp())
                .build();
        } catch (Exception e) {
            log.error("Failed to deserialize content for memory entry: {}", entity.getId(), e);
            throw new RuntimeException("Failed to deserialize memory entry: " + entity.getId(), e);
        }
    }
    
    /**
     * Helper method to clear all memory entries for a session (useful for testing or session deletion)
     */
    @Transactional
    public void clearSessionMemory(String sessionId) {
        log.debug("Clearing memory entries for session: {}", sessionId);
        repository.deleteBySessionId(sessionId);
    }
}