package engineer.hyper.rag.dtos;

import com.google.adk.sessions.Session;
import java.util.List;

/**
 * Serializable DTO for session list responses
 */
public record SessionListDto(List<Session> sessions) {}