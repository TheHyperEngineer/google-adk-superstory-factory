package engineer.hyper.rag.dtos;

import com.google.adk.events.Event;
import java.util.List;

/**
 * Serializable DTO for event list responses
 */
public record EventListDto(List<Event> events) {}