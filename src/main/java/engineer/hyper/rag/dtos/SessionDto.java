package engineer.hyper.rag.dtos;

import com.google.adk.sessions.Session;

/**
 * Serializable DTO for single session responses
 */
public record SessionDto(Session session) {}