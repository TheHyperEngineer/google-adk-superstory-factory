package engineer.hyper.rag;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.adk.events.Event;
import com.google.genai.types.Content;
import com.google.genai.types.Part;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.WebTestClient;
import java.util.List;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AdkIntegrationTest {

    @Autowired
    private WebTestClient webClient;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testFullSessionLifecycle() {
        String appName = "testApp";
        String userId = "user123";
        String sessionId = "test-session-001";

        // 1. Create session
        webClient.post()
            .uri(uriBuilder -> uriBuilder
                .path("/api/sessions/create")
                .queryParam("appName", appName)
                .queryParam("userId", userId)
                .queryParam("sessionId", sessionId)
                .build())
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.id").isEqualTo(sessionId);

        // 2. Add event
        Event event = Event.builder()
            .id("evt-001")
            .author("user")
            .content(Content.fromParts(Part.fromText("Hello ADK")))
            .build();

        webClient.post()
            .uri(uriBuilder -> uriBuilder
                .path("/api/sessions/{sessionId}/events")
                .queryParam("appName", appName)
                .queryParam("userId", userId)
                .build(sessionId))
            .bodyValue(event)
            .exchange()
            .expectStatus().isOk();

        // 3. Get session
        webClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/api/sessions/{sessionId}")
                .queryParam("appName", appName)
                .queryParam("userId", userId)
                .build(sessionId))
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.events.length()").isEqualTo(1);

        // 4. Add to memory
        webClient.post()
            .uri("/api/memory/add/{sessionId}", sessionId)
            .bodyValue(event) // Simplified - in real code fetch full session
            .exchange()
            .expectStatus().isOk();

        // 5. Search memory
        webClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/api/memory/search")
                .queryParam("appName", appName)
                .queryParam("userId", userId)
                .queryParam("query", "ADK")
                .build())
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.memories.length()").isEqualTo(1);

        // 6. Delete session
        webClient.delete()
            .uri(uriBuilder -> uriBuilder
                .path("/api/sessions/{sessionId}")
                .queryParam("appName", appName)
                .queryParam("userId", userId)
                .build(sessionId))
            .exchange()
            .expectStatus().isOk();
    }
}