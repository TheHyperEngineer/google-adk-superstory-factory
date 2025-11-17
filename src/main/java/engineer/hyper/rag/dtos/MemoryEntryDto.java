package engineer.hyper.rag.dtos;

import com.google.genai.types.Content;

public record MemoryEntryDto(
    Content content,
    String author,
    String timestamp
) {}